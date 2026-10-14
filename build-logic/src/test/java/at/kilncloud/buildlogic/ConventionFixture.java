package at.kilncloud.buildlogic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

/**
 * Throw-away Gradle project that applies the KilnCloud convention plugins under test.
 *
 * <p>The fixture uses the real version catalog and the real, strict dependency verification
 * metadata. Artifacts are downloaded from Maven Central and must match the pinned SHA-256 values,
 * so the tests do not depend on what an earlier build left in the Gradle user home.
 */
final class ConventionFixture {
    private final Path projectDir;

    /**
     * Creates a fixture project with the given plugins applied.
     *
     * @param projectDir empty temporary directory owned by the calling test
     * @param pluginIds ids of the convention plugins to apply, in order
     * @throws IOException if the project files cannot be written
     */
    ConventionFixture(Path projectDir, String... pluginIds) throws IOException {
        this.projectDir = projectDir;
        StringBuilder plugins = new StringBuilder("plugins {\n    `java-library`\n");
        for (String id : pluginIds) {
            plugins.append("    id(\"").append(id).append("\")\n");
        }
        plugins.append("}\n");
        // Same version catalog as the real build, so tests and production resolve identical versions.
        Files.copy(Path.of(System.getProperty("kiln.versionCatalog")), projectDir.resolve("libs.versions.toml"));
        // Same hash pinning as the real build: artifacts are downloaded from Maven Central, never trusted blindly.
        Files.createDirectories(projectDir.resolve("gradle"));
        Files.copy(
                Path.of(System.getProperty("kiln.verificationMetadata")),
                projectDir.resolve("gradle/verification-metadata.xml"));
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
                dependencyResolutionManagement {
                    repositories { mavenCentral() }
                    versionCatalogs { create("libs") { from(files("libs.versions.toml")) } }
                }
                rootProject.name = "fixture"
                """);
        Files.writeString(projectDir.resolve("build.gradle.kts"), plugins + """
                tasks.register("printRelease") {
                    val release = tasks.compileJava.flatMap { it.options.release }
                    doLast { println("release=" + release.get()) }
                }
                """);
    }

    /**
     * Writes a Java source file below {@code src/main/java}.
     *
     * @param relativePath path relative to {@code src/main/java}, e.g. {@code demo/Demo.java}
     * @param content full file content
     * @throws IOException if the file cannot be written
     */
    void mainSource(String relativePath, String content) throws IOException {
        write("src/main/java/" + relativePath, content);
    }

    /**
     * Writes a Java source file below {@code src/test/java}.
     *
     * @param relativePath path relative to {@code src/test/java}
     * @param content full file content
     * @throws IOException if the file cannot be written
     */
    void testSource(String relativePath, String content) throws IOException {
        write("src/test/java/" + relativePath, content);
    }

    /**
     * Runs Gradle and expects success.
     *
     * @param arguments Gradle tasks and options
     * @return the build result
     */
    BuildResult run(String... arguments) {
        return runner(arguments).build();
    }

    /**
     * Runs Gradle and expects the build to fail.
     *
     * @param arguments Gradle tasks and options
     * @return the build result of the failed build
     */
    BuildResult runAndFail(String... arguments) {
        return runner(arguments).buildAndFail();
    }

    private GradleRunner runner(String... arguments) {
        List<String> args = new ArrayList<>(List.of(arguments));
        args.add("--stacktrace");
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withTestKitDir(new File(System.getProperty("kiln.gradleUserHome")))
                .withArguments(args);
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = projectDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
