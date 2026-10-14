package at.kilncloud.buildlogic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Functional tests for the gates of {@code kiln.quality}: they must fail on violations. */
final class QualityConventionTest {
    @TempDir
    Path dir;

    @Test
    @DisplayName("kiln.quality: NullAway lässt das Dereferenzieren von null nicht durch")
    void quality_nullAwayRejectsNullDereference() throws IOException {
        ConventionFixture fixture = new ConventionFixture(dir, "kiln.java-cloud", "kiln.quality");
        fixture.mainSource("at/kilncloud/demo/Bad.java", """
                package at.kilncloud.demo;

                /** Dereferences null. */
                public final class Bad {
                    private Bad() {}

                    /**
                     * Fails.
                     *
                     * @return length
                     */
                    public static int length() {
                        String value = null;
                        return value.length();
                    }
                }
                """);
        BuildResult result = fixture.runAndFail("compileJava");
        assertTrue(result.getOutput().contains("NullAway"), result.getOutput());
    }

    @Test
    @DisplayName("kiln.quality: Spotless lehnt falsch formatierten Code ab")
    void quality_spotlessRejectsBadFormatting() throws IOException {
        ConventionFixture fixture = new ConventionFixture(dir, "kiln.java-cloud", "kiln.quality");
        fixture.mainSource("at/kilncloud/demo/Ugly.java", """
                package at.kilncloud.demo;
                /** Ugly. */
                public final class Ugly { private Ugly() {} }
                """);
        BuildResult result = fixture.runAndFail("spotlessCheck");
        assertTrue(result.getOutput().contains("format violations"), result.getOutput());
    }

    @Test
    @DisplayName("kiln.quality: JaCoCo verlangt 90 % Line- und Branch-Coverage")
    void quality_jacocoRejectsUncoveredCode() throws IOException {
        ConventionFixture fixture = new ConventionFixture(dir, "kiln.java-cloud", "kiln.quality");
        fixture.mainSource("at/kilncloud/demo/Logic.java", """
                package at.kilncloud.demo;

                /** Logic without full tests. */
                public final class Logic {
                    private Logic() {}

                    /**
                     * Picks a value.
                     *
                     * @param input input
                     * @return the input if positive, else zero
                     */
                    public static int positive(int input) {
                        if (input > 0) {
                            return input;
                        }
                        return 0;
                    }
                }
                """);
        fixture.testSource("at/kilncloud/demo/LogicTest.java", """
                package at.kilncloud.demo;

                import org.junit.jupiter.api.Test;

                /** Covers only part of the logic. */
                final class LogicTest {
                    /** Calls the method for the positive branch only. */
                    @Test
                    void positive_returnsInput() {
                        Logic.positive(1);
                    }
                }
                """);
        BuildResult result = fixture.runAndFail("jacocoTestCoverageVerification");
        assertTrue(result.getOutput().contains("Rule violated"), result.getOutput());
    }

    @Test
    @DisplayName("kiln.quality: sauberer, formatierter und getesteter Code besteht check")
    void quality_acceptsCleanModule() throws IOException {
        ConventionFixture fixture = new ConventionFixture(dir, "kiln.java-cloud", "kiln.quality");
        fixture.mainSource("at/kilncloud/demo/Good.java", """
                package at.kilncloud.demo;

                /** Good code. */
                public final class Good {
                    private Good() {}

                    /**
                     * Returns the answer.
                     *
                     * @return 42
                     */
                    public static int answer() {
                        return 42;
                    }
                }
                """);
        fixture.testSource("at/kilncloud/demo/GoodTest.java", """
                package at.kilncloud.demo;

                import static org.junit.jupiter.api.Assertions.assertEquals;

                import org.junit.jupiter.api.Test;

                /** Tests {@link Good}. */
                final class GoodTest {
                    /** Verifies the answer. */
                    @Test
                    void answer_returns42() {
                        assertEquals(42, Good.answer());
                    }
                }
                """);
        // The fixture sources are not hand-formatted; Spotless formats them, then check must pass.
        fixture.run("spotlessApply");
        fixture.run("check");
    }

    @Test
    @DisplayName("kiln.quality: Produktionscode ganz ohne Tests besteht die Coverage-Prüfung nicht")
    void quality_requiresCoverageDataForProductionCode() throws IOException {
        ConventionFixture fixture = new ConventionFixture(dir, "kiln.java-cloud", "kiln.quality");
        fixture.mainSource("at/kilncloud/demo/Untested.java", """
                package at.kilncloud.demo;

                /** Code without any test. */
                public final class Untested {
                    private Untested() {}
                }
                """);
        BuildResult result = fixture.runAndFail("jacocoTestCoverageVerification");
        assertTrue(result.getOutput().contains("no coverage data"), result.getOutput());
    }

    @Test
    @DisplayName("kiln.quality: ein Modul ohne Produktionscode besteht die Coverage-Prüfung")
    void quality_acceptsModuleWithoutProductionCode() throws IOException {
        ConventionFixture fixture = new ConventionFixture(dir, "kiln.java-cloud", "kiln.quality");
        fixture.mainSource("at/kilncloud/demo/package-info.java", """
                /** Metadata only. */
                package at.kilncloud.demo;
                """);
        fixture.run("jacocoTestCoverageVerification");
    }
}
