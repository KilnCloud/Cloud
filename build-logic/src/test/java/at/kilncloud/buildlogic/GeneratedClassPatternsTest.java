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

/** Tests {@code GeneratedClassPatterns.excludePatterns}. */
final class GeneratedClassPatternsTest {
    @TempDir
    Path root;

    private void write(String relativePath) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class X {}");
    }

    @Test
    @DisplayName("excludePatterns: fehlendes Verzeichnis ergibt keine Muster")
    void excludePatterns_missingDirectoryIsEmpty() {
        assertTrue(GeneratedClassPatterns.INSTANCE
                .excludePatterns(root.resolve("missing").toFile())
                .isEmpty());
    }

    @Test
    @DisplayName("excludePatterns: Quelle ergibt Klassen- und Nested-Klassen-Muster")
    void excludePatterns_sourceYieldsClassAndNestedPatterns() throws IOException {
        write("at/kilncloud/protocol/v1/RegisterRequest.java");
        assertEquals(
                List.of(
                        "at/kilncloud/protocol/v1/RegisterRequest$*.class",
                        "at/kilncloud/protocol/v1/RegisterRequest.class"),
                GeneratedClassPatterns.INSTANCE.excludePatterns(root.toFile()));
    }

    @Test
    @DisplayName("excludePatterns: Nicht-Java-Dateien werden ignoriert")
    void excludePatterns_nonJavaFilesAreIgnored() throws IOException {
        write("at/kilncloud/notes.txt");
        assertTrue(GeneratedClassPatterns.INSTANCE.excludePatterns(root.toFile()).isEmpty());
    }

    @Test
    @DisplayName("excludePatterns: mehrere Quellen werden sortiert geliefert")
    void excludePatterns_multipleSourcesAreSorted() throws IOException {
        write("b/Zeta.java");
        write("a/Alpha.java");
        assertEquals(
                List.of("a/Alpha$*.class", "a/Alpha.class", "b/Zeta$*.class", "b/Zeta.class"),
                GeneratedClassPatterns.INSTANCE.excludePatterns(root.toFile()));
    }
}
