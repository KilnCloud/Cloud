package at.kilncloud.common.filesystem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests {@link SecureFiles} on in-memory Unix and Windows file systems. */
final class SecureFilesTest {
    private FileSystem unix;
    private Path dir;

    @BeforeEach
    void setUp() throws IOException {
        unix = Jimfs.newFileSystem(Configuration.unix().toBuilder()
                .setAttributeViews("basic", "posix")
                .build());
        dir = Files.createDirectories(unix.getPath("/srv/kiln"));
    }

    @AfterEach
    void tearDown() throws IOException {
        unix.close();
    }

    private List<String> names(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.map(
                            path -> Objects.requireNonNull(path.getFileName()).toString())
                    .sorted()
                    .toList();
        }
    }

    @Test
    @DisplayName("readBounded liest den vollständigen Inhalt einer Datei")
    void readBounded_returnsContent() throws IOException {
        Path file = Files.writeString(dir.resolve("a.json"), "{\"a\":1}");
        assertArrayEquals("{\"a\":1}".getBytes(StandardCharsets.UTF_8), SecureFiles.readBounded(file, 1024));
    }

    @Test
    @DisplayName("readBounded liest eine leere Datei als leeres Array")
    void readBounded_emptyFileIsEmptyArray() throws IOException {
        Path file = Files.createFile(dir.resolve("empty"));
        assertEquals(0, SecureFiles.readBounded(file, 16).length);
    }

    @Test
    @DisplayName("readBounded akzeptiert eine Datei genau in Höhe des Limits")
    void readBounded_acceptsFileExactlyAtLimit() throws IOException {
        Path file = Files.write(dir.resolve("limit"), new byte[8]);
        assertEquals(8, SecureFiles.readBounded(file, 8).length);
    }

    @Test
    @DisplayName("readBounded lehnt eine Datei über dem Limit ab")
    void readBounded_rejectsFileAboveLimit() throws IOException {
        Path file = Files.write(dir.resolve("big"), new byte[9]);
        IOException failure = assertThrows(IOException.class, () -> SecureFiles.readBounded(file, 8));
        assertEquals("filesystem.file.too-large", failure.getMessage());
    }

    @Test
    @DisplayName("readBounded folgt keinem Symlink am Dateiende")
    void readBounded_rejectsSymbolicLink() throws IOException {
        Path secret = Files.writeString(dir.resolve("secret"), "top");
        Path link = Files.createSymbolicLink(dir.resolve("link"), secret);
        assertThrows(IOException.class, () -> SecureFiles.readBounded(link, 1024));
    }

    @Test
    @DisplayName("readBounded meldet eine fehlende Datei")
    void readBounded_missingFileFails() {
        assertThrows(NoSuchFileException.class, () -> SecureFiles.readBounded(dir.resolve("missing"), 16));
    }

    @Test
    @DisplayName("readBounded lehnt null und ein nicht positives Limit ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void readBounded_rejectsInvalidArguments() throws IOException {
        Path file = Files.createFile(dir.resolve("f"));
        assertThrows(NullPointerException.class, () -> SecureFiles.readBounded(null, 16));
        assertThrows(IllegalArgumentException.class, () -> SecureFiles.readBounded(file, 0));
        assertThrows(IllegalArgumentException.class, () -> SecureFiles.readBounded(file, -1));
        assertThrows(IllegalArgumentException.class, () -> SecureFiles.readBounded(file, Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("writeAtomically legt eine neue Datei mit dem Inhalt an")
    void writeAtomically_createsFile() throws IOException {
        Path target = dir.resolve("config.json");
        SecureFiles.writeAtomically(target, "neu".getBytes(StandardCharsets.UTF_8), false);
        assertEquals("neu", Files.readString(target));
    }

    @Test
    @DisplayName("writeAtomically ersetzt eine vorhandene Datei vollständig")
    void writeAtomically_replacesExistingFile() throws IOException {
        Path target = Files.writeString(dir.resolve("config.json"), "alt und deutlich länger");
        SecureFiles.writeAtomically(target, "neu".getBytes(StandardCharsets.UTF_8), false);
        assertEquals("neu", Files.readString(target));
    }

    @Test
    @DisplayName("writeAtomically hinterlässt keine temporären Dateien")
    void writeAtomically_leavesNoTemporaryFiles() throws IOException {
        SecureFiles.writeAtomically(dir.resolve("config.json"), new byte[] {1}, false);
        assertEquals(List.of("config.json"), names(dir));
    }

    @Test
    @DisplayName("writeAtomically schreibt eine leere Datei")
    void writeAtomically_emptyContent() throws IOException {
        Path target = dir.resolve("empty");
        SecureFiles.writeAtomically(target, new byte[0], false);
        assertEquals(0, Files.size(target));
    }

    @Test
    @DisplayName("writeAtomically setzt bei ownerOnly die Rechte 600")
    void writeAtomically_ownerOnlySetsPermission600() throws IOException {
        Path target = dir.resolve("secret.json");
        SecureFiles.writeAtomically(target, new byte[] {1}, true);
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(target)));
    }

    @Test
    @DisplayName("writeAtomically verweigert ownerOnly auf einem Dateisystem ohne POSIX-Rechte")
    void writeAtomically_ownerOnlyRejectsFileSystemWithoutPosix() throws IOException {
        try (FileSystem windows = Jimfs.newFileSystem(Configuration.windows())) {
            Path target = Files.createDirectories(windows.getPath("C:\\kiln")).resolve("secret.json");
            assertThrows(
                    UnsupportedOperationException.class, () -> SecureFiles.writeAtomically(target, new byte[1], true));
            assertFalse(Files.exists(target));
        }
    }

    @Test
    @DisplayName("writeAtomically ohne ownerOnly funktioniert auch ohne POSIX-Rechte")
    void writeAtomically_withoutOwnerOnlyWorksOnWindowsFileSystem() throws IOException {
        try (FileSystem windows = Jimfs.newFileSystem(Configuration.windows())) {
            Path target = Files.createDirectories(windows.getPath("C:\\kiln")).resolve("config.json");
            SecureFiles.writeAtomically(target, new byte[] {7}, false);
            assertArrayEquals(new byte[] {7}, Files.readAllBytes(target));
        }
    }

    @Test
    @DisplayName("writeAtomically ersetzt einen Symlink am Ziel, statt durch ihn zu schreiben")
    void writeAtomically_replacesSymlinkInsteadOfFollowing() throws IOException {
        Path victim = Files.writeString(dir.resolve("victim"), "unberührt");
        Path link = Files.createSymbolicLink(dir.resolve("config.json"), victim);
        SecureFiles.writeAtomically(link, "neu".getBytes(StandardCharsets.UTF_8), false);
        assertEquals("unberührt", Files.readString(victim));
        assertFalse(Files.isSymbolicLink(link));
        assertEquals("neu", Files.readString(link));
    }

    @Test
    @DisplayName("writeAtomically räumt die temporäre Datei bei einem Fehler auf")
    void writeAtomically_cleansUpWhenMoveFails() throws IOException {
        Path target = Files.createDirectories(dir.resolve("occupied"));
        Files.createFile(target.resolve("child"));
        assertThrows(IOException.class, () -> SecureFiles.writeAtomically(target, new byte[] {1}, false));
        assertEquals(List.of("occupied"), names(dir));
    }

    @Test
    @DisplayName("writeAtomically meldet ein fehlendes Elternverzeichnis")
    void writeAtomically_missingParentFails() {
        assertThrows(
                IOException.class,
                () -> SecureFiles.writeAtomically(dir.resolve("nope").resolve("f"), new byte[1], false));
    }

    @Test
    @DisplayName("writeAtomically lehnt ein Ziel ohne Elternverzeichnis ab")
    void writeAtomically_rootTargetFails() {
        IOException failure = assertThrows(
                IOException.class, () -> SecureFiles.writeAtomically(unix.getPath("/"), new byte[1], false));
        assertEquals("filesystem.target.no-parent", failure.getMessage());
    }

    @Test
    @DisplayName("writeAtomically lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void writeAtomically_rejectsNull() {
        assertThrows(NullPointerException.class, () -> SecureFiles.writeAtomically(null, new byte[1], false));
        assertThrows(NullPointerException.class, () -> SecureFiles.writeAtomically(dir.resolve("x"), null, false));
    }

    @Test
    @DisplayName("readBounded folgt auch auf dem echten Dateisystem keinem Symlink")
    void readBounded_rejectsSymbolicLinkOnDefaultFileSystem(@TempDir Path real) throws IOException {
        assumeTrue(real.getFileSystem().supportedFileAttributeViews().contains("posix"), "needs POSIX");
        Path secret = Files.writeString(real.resolve("secret"), "top");
        Path link;
        try {
            link = Files.createSymbolicLink(real.resolve("link"), secret);
        } catch (UnsupportedOperationException | IOException cannotLink) {
            assumeTrue(false, "symbolic links are not available");
            return;
        }
        assertThrows(IOException.class, () -> SecureFiles.readBounded(link, 1024));
    }

    @Test
    @DisplayName("writeAtomically setzt auf dem echten Dateisystem die Rechte 600")
    void writeAtomically_ownerOnlyOnDefaultFileSystem(@TempDir Path real) throws IOException {
        assumeTrue(real.getFileSystem().supportedFileAttributeViews().contains("posix"), "needs POSIX");
        Path target = real.resolve("secret.json");
        SecureFiles.writeAtomically(target, new byte[] {1}, true);
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(target)));
    }
}
