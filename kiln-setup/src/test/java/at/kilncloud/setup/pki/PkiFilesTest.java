package at.kilncloud.setup.pki;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.setup.TestClock;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link PkiFiles} on an in-memory Unix file system. */
final class PkiFilesTest {
    private static final Instant START = Instant.parse("2026-10-14T12:00:00Z");
    private final TestClock clock = new TestClock(START);
    private final CertificateAuthority authority =
            CertificateAuthority.create("KilnCloud CA", Duration.ofDays(365), clock);

    private FileSystem unix;
    private Path dir;

    @BeforeEach
    void setUp() throws IOException {
        unix = Jimfs.newFileSystem(Configuration.unix().toBuilder()
                .setAttributeViews("basic", "posix")
                .build());
        dir = Files.createDirectories(unix.getPath("/etc/kiln/pki"));
    }

    @AfterEach
    void tearDown() throws IOException {
        unix.close();
    }

    @Test
    @DisplayName("writeCertificate und readCertificate bilden ein Zertifikat verlustfrei ab")
    void certificate_roundTrips() throws Exception {
        Path file = dir.resolve("ca.crt");
        PkiFiles.writeCertificate(authority.certificate(), file);
        assertTrue(Files.readString(file).startsWith("-----BEGIN CERTIFICATE-----"));
        assertEquals(authority.certificate(), PkiFiles.readCertificate(file));
    }

    @Test
    @DisplayName("writePrivateKey setzt Rechte 600 und readPrivateKey liest den Schlüssel wieder")
    void privateKey_roundTripsWithOwnerOnlyPermissions() throws IOException {
        Path file = dir.resolve("node.key");
        PrivateKey key = authority
                .issueClient(new NodeName("node-1"), Duration.ofDays(1))
                .privateKey();
        PkiFiles.writePrivateKey(key, file, true);
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
        assertTrue(Files.readString(file).startsWith("-----BEGIN PRIVATE KEY-----"));
        assertEquals(key, PkiFiles.readPrivateKey(file));
    }

    @Test
    @DisplayName("writePrivateKey verweigert Rechte 600 auf einem Dateisystem ohne POSIX")
    void writePrivateKey_restrictedNeedsPosix() throws IOException {
        PrivateKey key = authority.privateKey();
        try (FileSystem windows = Jimfs.newFileSystem(Configuration.windows())) {
            Path file = Files.createDirectories(windows.getPath("C:\\kiln")).resolve("ca.key");
            assertThrows(UnsupportedOperationException.class, () -> PkiFiles.writePrivateKey(key, file, true));
            assertFalse(Files.exists(file));
            PkiFiles.writePrivateKey(key, file, false);
            assertTrue(Files.exists(file));
        }
    }

    @Test
    @DisplayName("writeAuthority und readAuthority stellen eine funktionierende CA wieder her")
    void authority_roundTrips() throws IOException {
        Path certificate = dir.resolve("ca.crt");
        Path key = dir.resolve("ca.key");
        PkiFiles.writeAuthority(authority, certificate, key, true);
        CertificateAuthority restored = PkiFiles.readAuthority(certificate, key, clock);
        X509Certificate leaf =
                restored.issueClient(new NodeName("node-1"), Duration.ofDays(1)).certificate();
        assertDoesNotThrow(() -> leaf.verify(authority.certificate().getPublicKey()));
    }

    @Test
    @DisplayName("readAuthority lehnt einen Schlüssel ab, der nicht zum Zertifikat gehört")
    void readAuthority_rejectsMismatchingKey() throws IOException {
        CertificateAuthority other = CertificateAuthority.create("Other CA", Duration.ofDays(1), clock);
        Path certificate = dir.resolve("ca.crt");
        Path key = dir.resolve("ca.key");
        PkiFiles.writeCertificate(authority.certificate(), certificate);
        PkiFiles.writePrivateKey(other.privateKey(), key, true);
        assertThrows(IllegalArgumentException.class, () -> PkiFiles.readAuthority(certificate, key, clock));
    }

    @Test
    @DisplayName("readCertificate lehnt Dateien ab, die kein Zertifikat sind")
    void readCertificate_rejectsInvalidFiles() throws IOException {
        Path garbage = Files.writeString(dir.resolve("garbage.crt"), "kein pem");
        Path empty = Files.createFile(dir.resolve("empty.crt"));
        Path wrongType = dir.resolve("wrong.crt");
        PkiFiles.writePrivateKey(authority.privateKey(), wrongType, true);
        Path badBody = Files.writeString(
                dir.resolve("bad.crt"), "-----BEGIN CERTIFICATE-----\nAAAA\n-----END CERTIFICATE-----\n");
        for (Path file : new Path[] {garbage, empty, wrongType, badBody}) {
            IOException failure = assertThrows(IOException.class, () -> PkiFiles.readCertificate(file));
            assertEquals("pki.pem.invalid", failure.getMessage(), file.toString());
        }
    }

    @Test
    @DisplayName("readCertificate lehnt eine Datei mit mehreren PEM-Blöcken ab")
    void readCertificate_rejectsMultipleObjects() throws IOException {
        Path first = dir.resolve("one.crt");
        PkiFiles.writeCertificate(authority.certificate(), first);
        String single = Files.readString(first);
        Path both = Files.writeString(dir.resolve("two.crt"), single + single);
        assertThrows(IOException.class, () -> PkiFiles.readCertificate(both));
    }

    @Test
    @DisplayName("readPrivateKey lehnt Dateien ab, die kein Schlüssel sind")
    void readPrivateKey_rejectsInvalidFiles() throws IOException {
        Path certificate = dir.resolve("ca.crt");
        PkiFiles.writeCertificate(authority.certificate(), certificate);
        Path badBody = Files.writeString(
                dir.resolve("bad.key"), "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n");
        assertThrows(IOException.class, () -> PkiFiles.readPrivateKey(certificate));
        assertThrows(IOException.class, () -> PkiFiles.readPrivateKey(badBody));
    }

    @Test
    @DisplayName("Lesen lehnt Symlinks und zu große Dateien ab")
    void read_rejectsSymlinksAndOversizedFiles() throws IOException {
        Path real = dir.resolve("ca.crt");
        PkiFiles.writeCertificate(authority.certificate(), real);
        Path link = Files.createSymbolicLink(dir.resolve("link.crt"), real);
        Path huge = Files.write(dir.resolve("huge.crt"), "A".repeat(70_000).getBytes(StandardCharsets.US_ASCII));
        assertThrows(IOException.class, () -> PkiFiles.readCertificate(link));
        assertThrows(IOException.class, () -> PkiFiles.readCertificate(huge));
    }

    @Test
    @DisplayName("Die Methoden lehnen null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void methods_rejectNull() {
        Path file = dir.resolve("x");
        assertThrows(NullPointerException.class, () -> PkiFiles.writeCertificate(null, file));
        assertThrows(NullPointerException.class, () -> PkiFiles.writeCertificate(authority.certificate(), null));
        assertThrows(NullPointerException.class, () -> PkiFiles.writePrivateKey(null, file, true));
        assertThrows(NullPointerException.class, () -> PkiFiles.writePrivateKey(authority.privateKey(), null, true));
        assertThrows(NullPointerException.class, () -> PkiFiles.readCertificate(null));
        assertThrows(NullPointerException.class, () -> PkiFiles.readPrivateKey(null));
        assertThrows(NullPointerException.class, () -> PkiFiles.writeAuthority(null, file, file, true));
    }
}
