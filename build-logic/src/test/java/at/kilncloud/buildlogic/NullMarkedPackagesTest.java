package at.kilncloud.buildlogic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests {@code NullMarkedPackages.violations}. */
final class NullMarkedPackagesTest {
    private static final String MARKED = """
            /** Documented. */
            @NullMarked
            package demo.ok;

            import org.jspecify.annotations.NullMarked;
            """;

    @TempDir
    Path root;

    private void write(String relativePath, String content) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private List<String> violations() {
        return NullMarkedPackages.INSTANCE.violations(root.toFile());
    }

    @Test
    @DisplayName("violations: fehlendes Quellverzeichnis hat keine Verstöße")
    void violations_missingRootIsEmpty() {
        assertTrue(NullMarkedPackages.INSTANCE.violations(root.resolve("missing").toFile()).isEmpty());
    }

    @Test
    @DisplayName("violations: markiertes Paket ist konform")
    void violations_markedPackageIsCompliant() throws IOException {
        write("demo/ok/Code.java", "package demo.ok; class Code {}");
        write("demo/ok/package-info.java", MARKED);
        assertTrue(violations().isEmpty());
    }

    @Test
    @DisplayName("violations: Paket mit Code ohne package-info wird gemeldet")
    void violations_missingPackageInfoIsReported() throws IOException {
        write("demo/bad/Code.java", "package demo.bad; class Code {}");
        assertEquals(List.of("demo/bad: package-info.java is missing"), violations());
    }

    @Test
    @DisplayName("violations: package-info ohne @NullMarked wird gemeldet")
    void violations_unmarkedPackageInfoIsReported() throws IOException {
        write("demo/bad/Code.java", "package demo.bad; class Code {}");
        write("demo/bad/package-info.java", "/** Docs only. */\npackage demo.bad;\n");
        assertEquals(List.of("demo/bad: package-info.java does not declare @NullMarked"), violations());
    }

    @Test
    @DisplayName("violations: auskommentiertes @NullMarked zählt nicht")
    void violations_commentedAnnotationIsReported() throws IOException {
        write("demo/bad/Code.java", "package demo.bad; class Code {}");
        write("demo/bad/package-info.java", """
                /**
                 * @NullMarked
                 */
                // @NullMarked
                package demo.bad;

                import org.jspecify.annotations.NullMarked;
                """);
        assertEquals(List.of("demo/bad: package-info.java does not declare @NullMarked"), violations());
    }

    @Test
    @DisplayName("violations: @NullMarked ohne JSpecify-Import zählt nicht")
    void violations_annotationWithoutJspecifyImportIsReported() throws IOException {
        write("demo/bad/Code.java", "package demo.bad; class Code {}");
        write("demo/bad/package-info.java", "@NullMarked\npackage demo.bad;\n");
        assertEquals(List.of("demo/bad: package-info.java does not declare @NullMarked"), violations());
    }

    @Test
    @DisplayName("violations: Verzeichnisse nur mit module-info oder package-info werden ignoriert")
    void violations_metadataOnlyDirectoriesAreIgnored() throws IOException {
        write("module-info.java", "module demo {}");
        write("demo/meta/package-info.java", "package demo.meta;\n");
        assertTrue(violations().isEmpty());
    }

    @Test
    @DisplayName("violations: mehrere Verstöße werden sortiert gemeldet")
    void violations_multipleViolationsAreSorted() throws IOException {
        write("demo/z/Code.java", "class Code {}");
        write("demo/a/Code.java", "class Code {}");
        assertEquals(
                List.of("demo/a: package-info.java is missing", "demo/z: package-info.java is missing"), violations());
    }
}
