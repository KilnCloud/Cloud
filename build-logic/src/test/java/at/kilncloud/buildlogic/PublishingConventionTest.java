package at.kilncloud.buildlogic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Functional tests for {@code kiln.publishing}. */
final class PublishingConventionTest {
    @TempDir
    Path dir;

    @Test
    @DisplayName("kiln.publishing erzeugt ein POM mit Apache-2.0-Lizenz und SCM-Angaben")
    void publishing_generatesPomWithLicenseAndScm() throws IOException {
        new ConventionFixture(dir, "kiln.java-compat", "kiln.publishing")
                .run("generatePomFileForMavenPublication");
        String pom = Files.readString(dir.resolve("build/publications/maven/pom-default.xml"));
        assertTrue(pom.contains("Apache-2.0"), pom);
        assertTrue(pom.contains("<scm>"), pom);
    }

    @Test
    @DisplayName("kiln.publishing konfiguriert kein Publikationsziel und keine Zugangsdaten")
    void publishing_configuresNoRepository() throws IOException {
        BuildResult result = new ConventionFixture(dir, "kiln.java-compat", "kiln.publishing")
                .run("tasks", "--all", "-q");
        assertTrue(result.getOutput().contains("publishToMavenLocal"), result.getOutput());
        assertFalse(result.getOutput().contains("MavenPublicationToMavenRepository"), result.getOutput());
    }
}
