package at.kilncloud.buildlogic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests {@code ProductionBytecode.existsIn}. */
final class ProductionBytecodeTest {
    @TempDir
    Path dir;

    private void touch(String relativePath) throws IOException {
        Path file = dir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[0]);
    }

    @Test
    @DisplayName("existsIn: fehlendes Verzeichnis enthält keinen Produktionscode")
    void existsIn_missingDirectoryIsFalse() {
        assertFalse(ProductionBytecode.INSTANCE.existsIn(dir.resolve("missing").toFile()));
    }

    @Test
    @DisplayName("existsIn: leeres Verzeichnis enthält keinen Produktionscode")
    void existsIn_emptyDirectoryIsFalse() {
        assertFalse(ProductionBytecode.INSTANCE.existsIn(dir.toFile()));
    }

    @Test
    @DisplayName("existsIn: module-info und package-info zählen nicht als Produktionscode")
    void existsIn_metadataOnlyIsFalse() throws IOException {
        touch("module-info.class");
        touch("at/kilncloud/common/internal/package-info.class");
        assertFalse(ProductionBytecode.INSTANCE.existsIn(dir.toFile()));
    }

    @Test
    @DisplayName("existsIn: jede weitere Klasse zählt als Produktionscode")
    void existsIn_regularClassIsTrue() throws IOException {
        touch("module-info.class");
        touch("at/kilncloud/master/internal/KilnMaster.class");
        assertTrue(ProductionBytecode.INSTANCE.existsIn(dir.toFile()));
    }

    @Test
    @DisplayName("existsIn: Nicht-Klassen-Dateien werden ignoriert")
    void existsIn_nonClassFilesAreIgnored() throws IOException {
        touch("at/kilncloud/notes.txt");
        assertFalse(ProductionBytecode.INSTANCE.existsIn(dir.toFile()));
    }

    @Test
    @DisplayName("existsIn: ein Verzeichnis namens *.class ist keine Klasse")
    void existsIn_directoryNamedLikeClassIsIgnored() throws IOException {
        Files.createDirectories(dir.resolve("Fake.class"));
        assertFalse(ProductionBytecode.INSTANCE.existsIn(dir.toFile()));
    }
}
