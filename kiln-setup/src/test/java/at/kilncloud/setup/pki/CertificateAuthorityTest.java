package at.kilncloud.setup.pki;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.setup.TestClock;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link CertificateAuthority}. */
final class CertificateAuthorityTest {
    private static final Instant START = Instant.parse("2026-10-14T12:00:00Z");
    private static final Duration CA_VALIDITY = Duration.ofDays(1825);
    private static final NodeName NODE = new NodeName("node-1");

    private final TestClock clock = new TestClock(START);
    private final CertificateAuthority authority = CertificateAuthority.create("KilnCloud CA", CA_VALIDITY, clock);

    private static boolean contains(Collection<List<?>> names, int type, String value) {
        return names.stream().anyMatch(n -> n.get(0).equals(type) && n.get(1).equals(value));
    }

    @Test
    @DisplayName("create erzeugt ein selbstsigniertes CA-Zertifikat mit Pfadlänge 0")
    void create_selfSignedCaWithPathLengthZero() {
        X509Certificate ca = authority.certificate();
        assertEquals(0, ca.getBasicConstraints());
        assertEquals(ca.getSubjectX500Principal(), ca.getIssuerX500Principal());
        assertTrue(ca.getKeyUsage()[5], "keyCertSign");
        assertDoesNotThrow(() -> ca.verify(ca.getPublicKey()));
    }

    @Test
    @DisplayName("create setzt die Gültigkeit nach der injizierten Uhr")
    void create_usesInjectedClockForValidity() {
        X509Certificate ca = authority.certificate();
        assertEquals(START.plus(CA_VALIDITY), ca.getNotAfter().toInstant());
        assertTrue(ca.getNotBefore().toInstant().isBefore(START));
    }

    @Test
    @DisplayName("create lehnt ungültige Argumente ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void create_rejectsInvalidArguments() {
        assertThrows(NullPointerException.class, () -> CertificateAuthority.create(null, CA_VALIDITY, clock));
        assertThrows(NullPointerException.class, () -> CertificateAuthority.create("CA", null, clock));
        assertThrows(NullPointerException.class, () -> CertificateAuthority.create("CA", CA_VALIDITY, null));
        assertThrows(IllegalArgumentException.class, () -> CertificateAuthority.create(" ", CA_VALIDITY, clock));
        assertThrows(
                IllegalArgumentException.class, () -> CertificateAuthority.create("a".repeat(65), CA_VALIDITY, clock));
        assertThrows(IllegalArgumentException.class, () -> CertificateAuthority.create("CA", Duration.ZERO, clock));
        assertThrows(
                IllegalArgumentException.class, () -> CertificateAuthority.create("CA", Duration.ofDays(-1), clock));
    }

    @Test
    @DisplayName("issueClient stellt ein von der CA signiertes Client-Zertifikat aus")
    void issueClient_signedClientCertificate() throws Exception {
        IssuedCertificate issued = authority.issueClient(NODE, Duration.ofDays(90));
        X509Certificate leaf = issued.certificate();
        assertDoesNotThrow(() -> leaf.verify(authority.certificate().getPublicKey()));
        assertEquals("CN=node-1", leaf.getSubjectX500Principal().getName());
        assertEquals(-1, leaf.getBasicConstraints(), "leaf must not be a CA");
        assertEquals(List.of("1.3.6.1.5.5.7.3.2"), leaf.getExtendedKeyUsage());
        assertTrue(leaf.getKeyUsage()[0], "digitalSignature");
        assertEquals(authority.certificate(), issued.authority());
        assertEquals(START.plus(Duration.ofDays(90)), leaf.getNotAfter().toInstant());
    }

    @Test
    @DisplayName("issueClient vergibt jedes Mal eine andere Seriennummer und einen anderen Schlüssel")
    void issueClient_uniqueSerialAndKey() {
        IssuedCertificate first = authority.issueClient(NODE, Duration.ofDays(1));
        IssuedCertificate second = authority.issueClient(NODE, Duration.ofDays(1));
        assertNotEquals(
                first.certificate().getSerialNumber(), second.certificate().getSerialNumber());
        assertNotEquals(first.certificate().getPublicKey(), second.certificate().getPublicKey());
    }

    @Test
    @DisplayName("issueClient beschränkt die Gültigkeit auf das Ende der CA")
    void issueClient_neverOutlivesAuthority() {
        CertificateAuthority shortLived = CertificateAuthority.create("Short CA", Duration.ofDays(10), clock);
        IssuedCertificate issued = shortLived.issueClient(NODE, Duration.ofDays(300));
        assertEquals(
                shortLived.certificate().getNotAfter(), issued.certificate().getNotAfter());
    }

    @Test
    @DisplayName("issueClient lehnt ungültige Gültigkeiten ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void issueClient_rejectsInvalidValidity() {
        assertThrows(IllegalArgumentException.class, () -> authority.issueClient(NODE, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> authority.issueClient(NODE, Duration.ofSeconds(-1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> authority.issueClient(NODE, CertificateAuthority.MAX_LEAF_VALIDITY.plusSeconds(1)));
        assertDoesNotThrow(() -> authority.issueClient(NODE, CertificateAuthority.MAX_LEAF_VALIDITY));
        assertThrows(NullPointerException.class, () -> authority.issueClient(null, Duration.ofDays(1)));
        assertThrows(NullPointerException.class, () -> authority.issueClient(NODE, null));
    }

    @Test
    @DisplayName("issueServer trägt DNS-Namen und IP-Adressen als Alternative Names ein")
    void issueServer_includesAlternativeNames() throws UnknownHostException, GeneralSecurityException {
        IssuedCertificate issued = authority.issueServer(
                new NodeName("master"),
                List.of("master.example.org", "localhost"),
                List.of(InetAddress.getByAddress(new byte[] {127, 0, 0, 1})),
                Duration.ofDays(30));
        X509Certificate server = issued.certificate();
        Collection<List<?>> names = server.getSubjectAlternativeNames();
        assertTrue(contains(names, 2, "master.example.org"));
        assertTrue(contains(names, 2, "localhost"));
        assertTrue(contains(names, 7, "127.0.0.1"));
        assertEquals(List.of("1.3.6.1.5.5.7.3.1"), server.getExtendedKeyUsage());
    }

    @Test
    @DisplayName("issueServer lehnt fehlende, zu viele und ungültige Namen ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void issueServer_rejectsInvalidNames() {
        NodeName master = new NodeName("master");
        Duration validity = Duration.ofDays(1);
        assertThrows(
                IllegalArgumentException.class, () -> authority.issueServer(master, List.of(), List.of(), validity));
        assertThrows(
                IllegalArgumentException.class,
                () -> authority.issueServer(master, List.of("bad_name!"), List.of(), validity));
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            many.add("h" + i + ".example.org");
        }
        assertThrows(IllegalArgumentException.class, () -> authority.issueServer(master, many, List.of(), validity));
        assertThrows(NullPointerException.class, () -> authority.issueServer(null, List.of("a"), List.of(), validity));
        assertThrows(NullPointerException.class, () -> authority.issueServer(master, null, List.of(), validity));
        assertThrows(NullPointerException.class, () -> authority.issueServer(master, List.of("a"), null, validity));
    }

    @Test
    @DisplayName("restore stellt eine CA aus Zertifikat und Schlüssel wieder her")
    void restore_returnsWorkingAuthority() {
        CertificateAuthority restored =
                CertificateAuthority.restore(authority.certificate(), authority.privateKey(), clock);
        IssuedCertificate issued = restored.issueClient(NODE, Duration.ofDays(1));
        assertDoesNotThrow(
                () -> issued.certificate().verify(authority.certificate().getPublicKey()));
    }

    @Test
    @DisplayName("restore lehnt ein Zertifikat ab, das keine CA ist")
    void restore_rejectsNonAuthorityCertificate() {
        IssuedCertificate leaf = authority.issueClient(NODE, Duration.ofDays(1));
        assertThrows(
                IllegalArgumentException.class,
                () -> CertificateAuthority.restore(leaf.certificate(), leaf.privateKey(), clock));
    }

    @Test
    @DisplayName("restore lehnt einen Schlüssel ab, der nicht zum Zertifikat gehört")
    void restore_rejectsMismatchingKey() {
        CertificateAuthority other = CertificateAuthority.create("Other CA", CA_VALIDITY, clock);
        assertThrows(
                IllegalArgumentException.class,
                () -> CertificateAuthority.restore(authority.certificate(), other.privateKey(), clock));
    }

    @Test
    @DisplayName("restore lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void restore_rejectsNull() {
        assertThrows(
                NullPointerException.class, () -> CertificateAuthority.restore(null, authority.privateKey(), clock));
        assertThrows(
                NullPointerException.class, () -> CertificateAuthority.restore(authority.certificate(), null, clock));
        assertThrows(
                NullPointerException.class,
                () -> CertificateAuthority.restore(authority.certificate(), authority.privateKey(), null));
    }

    @Test
    @DisplayName("toString der CA und der Identität enthält keinen Schlüssel")
    void toString_hidesKeyMaterial() {
        IssuedCertificate issued = authority.issueClient(NODE, Duration.ofDays(1));
        assertTrue(authority.toString().contains("privateKey=***"));
        assertTrue(issued.toString().contains("privateKey=***"));
        assertFalse(issued.toString().contains(issued.privateKey().getAlgorithm() + " "));
    }
}
