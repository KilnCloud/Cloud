package at.kilncloud.common.filesystem;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests lexical and real-path containment enforced by {@link SecurePathResolver}. */
final class SecurePathResolverTest {
    /** Temporary filesystem root supplied independently for each test. */
    @TempDir
    Path temporaryDirectory;

    /**
     * Verifies ordinary resolution to a canonical existing path.
     *
     * @throws IOException if test fixtures cannot be created or resolved
     */
    @Test
    @DisplayName("resolveExisting gibt den realen Pfad innerhalb der Basis zurück")
    void resolveExisting_returnsRealPathInsideBase() throws IOException {
        Path base = Files.createDirectory(temporaryDirectory.resolve("base"));
        Path target = Files.writeString(base.resolve("server.jar"), "fixture");

        assertEquals(target.toRealPath(), SecurePathResolver.resolveExisting(base, Path.of("server.jar")));
    }

    /**
     * Verifies normalization that remains inside the approved directory.
     *
     * @throws IOException if test fixtures cannot be created or resolved
     */
    @Test
    @DisplayName("resolveExisting normalisiert einen weiterhin internen Pfad")
    void resolveExisting_acceptsNormalizedPathInsideBase() throws IOException {
        Path base = Files.createDirectory(temporaryDirectory.resolve("base"));
        Files.createDirectory(base.resolve("unused"));
        Path target = Files.writeString(base.resolve("config.json"), "fixture");

        Path resolved = SecurePathResolver.resolveExisting(base, Path.of("unused", "..", "config.json"));

        assertEquals(target.toRealPath(), resolved);
    }

    /**
     * Verifies that caller-supplied absolute paths are denied even when they point inside.
     *
     * @throws IOException if test fixtures cannot be created
     */
    @Test
    @DisplayName("resolveExisting lehnt absolute Eingabepfade ab")
    void resolveExisting_rejectsAbsolutePath() throws IOException {
        Path base = Files.createDirectory(temporaryDirectory.resolve("base"));
        Path target = Files.writeString(base.resolve("inside.txt"), "fixture");

        assertThrows(IllegalArgumentException.class, () -> SecurePathResolver.resolveExisting(base, target));
    }

    /**
     * Verifies lexical parent traversal is denied before filesystem resolution.
     *
     * @throws IOException if test fixtures cannot be created
     */
    @Test
    @DisplayName("resolveExisting lehnt lexikalische Pfadtraversierung ab")
    void resolveExisting_rejectsLexicalTraversal() throws IOException {
        Path base = Files.createDirectory(temporaryDirectory.resolve("base"));
        Files.writeString(temporaryDirectory.resolve("outside.txt"), "fixture");

        assertThrows(
                IllegalArgumentException.class,
                () -> SecurePathResolver.resolveExisting(base, Path.of("..", "outside.txt")));
    }

    /**
     * Verifies a symlink cannot escape from the approved real directory.
     *
     * @throws IOException if test fixtures or the symbolic link cannot be created
     */
    @Test
    @DisplayName("resolveExisting lehnt einen Symlink-Ausbruch ab")
    void resolveExisting_rejectsSymlinkEscape() throws IOException {
        Path base = Files.createDirectory(temporaryDirectory.resolve("base"));
        Path outside = Files.createDirectory(temporaryDirectory.resolve("outside"));
        Files.writeString(outside.resolve("secret.txt"), "fixture");
        try {
            Files.createSymbolicLink(base.resolve("escape"), outside);
        } catch (FileSystemException | UnsupportedOperationException exception) {
            Assumptions.assumeTrue(false, "filesystem.symlink.unavailable");
            return;
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> SecurePathResolver.resolveExisting(base, Path.of("escape", "secret.txt")));
    }

    /**
     * Verifies a regular file cannot be used as the base directory.
     *
     * @throws IOException if the test fixture cannot be created
     */
    @Test
    @DisplayName("resolveExisting lehnt eine Datei als Basis ab")
    void resolveExisting_rejectsNonDirectoryBase() throws IOException {
        Path file = Files.writeString(temporaryDirectory.resolve("base.txt"), "fixture");

        assertThrows(NotDirectoryException.class, () -> SecurePathResolver.resolveExisting(file, Path.of("child")));
    }

    /**
     * Verifies a missing target remains an explicit filesystem error.
     *
     * @throws IOException if the base fixture cannot be created
     */
    @Test
    @DisplayName("resolveExisting lehnt einen fehlenden Zielpfad ab")
    void resolveExisting_rejectsMissingTarget() throws IOException {
        Path base = Files.createDirectory(temporaryDirectory.resolve("base"));

        assertThrows(NoSuchFileException.class, () -> SecurePathResolver.resolveExisting(base, Path.of("missing.txt")));
    }

    /** Verifies null cannot cross either filesystem boundary. */
    @Test
    @DisplayName("resolveExisting lehnt null für beide Parameter ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void resolveExisting_rejectsNullInputs() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class, () -> SecurePathResolver.resolveExisting(null, Path.of("target"))),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> SecurePathResolver.resolveExisting(temporaryDirectory, null)));
    }
}
