package at.kilncloud.buildlogic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Functional tests for {@code kiln.java-base}, {@code kiln.java-cloud} and {@code kiln.java-compat}. */
final class JavaConventionsTest {
    private static final String CLEAN_SOURCE = """
            package demo;

            /** Demo. */
            public final class Demo {
                private Demo() {}
            }
            """;

    @TempDir
    Path dir;

    @Test
    @DisplayName("kiln.java-cloud kompiliert mit --release 25")
    void javaCloud_setsRelease25() throws IOException {
        BuildResult result = new ConventionFixture(dir, "kiln.java-cloud").run("printRelease", "-q");
        assertTrue(result.getOutput().contains("release=25"), result.getOutput());
    }

    @Test
    @DisplayName("kiln.java-compat kompiliert mit --release 21")
    void javaCompat_setsRelease21() throws IOException {
        BuildResult result = new ConventionFixture(dir, "kiln.java-compat").run("printRelease", "-q");
        assertTrue(result.getOutput().contains("release=21"), result.getOutput());
    }

    @Test
    @DisplayName("kiln.java-base macht Compiler-Warnungen zu Fehlern (-Xlint:all -Werror)")
    void javaBase_failsBuildOnCompilerWarning() throws IOException {
        ConventionFixture fixture = new ConventionFixture(dir, "kiln.java-cloud");
        fixture.mainSource("demo/Raw.java", """
                package demo;

                import java.util.List;

                /** Uses a raw type. */
                public final class Raw {
                    /** Holds a raw list. */
                    public List raw;
                }
                """);
        BuildResult result = fixture.runAndFail("compileJava");
        assertTrue(result.getOutput().contains("raw"), result.getOutput());
    }

    @Test
    @DisplayName("kiln.java-base erzeugt reproduzierbare Archive ohne Zeitstempel")
    void javaBase_producesReproducibleJar() throws IOException {
        ConventionFixture fixture = new ConventionFixture(dir, "kiln.java-cloud");
        fixture.mainSource("demo/Demo.java", CLEAN_SOURCE);
        Path jar = dir.resolve("build/libs/fixture.jar");
        fixture.run("jar");
        byte[] first = Files.readAllBytes(jar);
        fixture.run("clean", "jar");
        assertArrayEquals(first, Files.readAllBytes(jar), "jar must be byte-identical across builds");
    }

    @Test
    @DisplayName("kiln.java-base fordert Javadoc nach -Xdoclint:all")
    void javaBase_failsJavadocOnMissingComment() throws IOException {
        ConventionFixture fixture = new ConventionFixture(dir, "kiln.java-cloud");
        fixture.mainSource("demo/Undocumented.java", """
                package demo;

                public final class Undocumented {
                    public int value;
                }
                """);
        fixture.runAndFail("javadoc");
    }
}
