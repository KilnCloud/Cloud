package at.kilncloud.master.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests the master process entry point through {@link KilnMaster#run}. */
final class KilnMasterTest {
    @TempDir
    Path dir;

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final PrintStream errors = new PrintStream(captured, true, StandardCharsets.UTF_8);

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("run verlangt genau ein Argument und nennt die Verwendung")
    void run_requiresExactlyOneArgument() {
        assertEquals(KilnMaster.EXIT_USAGE, KilnMaster.run(new String[0], errors, false));
        assertEquals(KilnMaster.EXIT_USAGE, KilnMaster.run(new String[] {"a", "b"}, errors, false));
        assertTrue(output().contains("master.usage"));
    }

    @Test
    @DisplayName("run meldet eine fehlende Konfigurationsdatei neutral")
    void run_reportsMissingConfigFile() {
        int code = KilnMaster.run(new String[] {dir.resolve("missing.json").toString()}, errors, false);
        assertEquals(KilnMaster.EXIT_FAILURE, code);
        assertTrue(output().contains("master.start.failed"));
    }

    @Test
    @DisplayName("run meldet eine ungültige Konfiguration mit dem Feldpfad, aber ohne Werte")
    void run_reportsInvalidConfigWithoutValues() throws IOException {
        Path file = Files.writeString(dir.resolve("master.json"), "{\"unbekannt\":\"geheimer-wert\"}");
        int code = KilnMaster.run(new String[] {file.toString()}, errors, false);
        assertEquals(KilnMaster.EXIT_FAILURE, code);
        assertTrue(output().contains("config.invalid"));
        assertTrue(!output().contains("geheimer-wert"));
    }

    @Test
    @DisplayName("run startet den Master mit gültiger Konfiguration und beendet ihn wieder")
    void run_startsAndStopsWithValidConfiguration() throws IOException {
        MasterFixture fixture = new MasterFixture(dir);
        Path config = fixture.writeConfig(MasterFixture.freePort());
        assertEquals(KilnMaster.EXIT_OK, KilnMaster.run(new String[] {config.toString()}, errors, false));
        assertTrue(Files.exists(dir.resolve("audit.log")));
    }

    @Test
    @DisplayName("run warnt, wenn die Dateirechte-Einschränkung abgeschaltet ist")
    void run_warnsAboutUnrestrictedFilePermissions() throws IOException {
        MasterFixture fixture = new MasterFixture(dir);
        Path config = dir.resolve("master.json");
        at.kilncloud.common.config.SecureConfig.write(config, fixture.config(MasterFixture.freePort(), true), false);
        assertEquals(KilnMaster.EXIT_OK, KilnMaster.run(new String[] {config.toString()}, errors, false));
        assertTrue(output().contains("master.warning.unrestricted-file-permissions"));
    }

    @Test
    @DisplayName("run wartet im blockierenden Modus und endet sauber, wenn der Thread unterbrochen wird")
    void run_blockingModeEndsOnInterrupt() throws IOException {
        MasterFixture fixture = new MasterFixture(dir);
        Path config = fixture.writeConfig(MasterFixture.freePort());
        Thread.currentThread().interrupt();
        try {
            assertEquals(KilnMaster.EXIT_OK, KilnMaster.run(new String[] {config.toString()}, errors, true));
            assertTrue(Thread.currentThread().isInterrupted(), "the interrupt flag must be preserved");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("run meldet einen belegten Port als Startfehler")
    void run_reportsPortInUse() throws IOException {
        MasterFixture fixture = new MasterFixture(dir);
        int port = MasterFixture.freePort();
        Path config = fixture.writeConfig(port);
        try (MasterServer running = MasterServer.start(fixture.config(port, false), dir, java.time.Clock.systemUTC())) {
            assertEquals(KilnMaster.EXIT_FAILURE, KilnMaster.run(new String[] {config.toString()}, errors, false));
            assertTrue(output().contains("master.start.failed"));
            assertTrue(running.port() == port);
        }
    }

    @Test
    @DisplayName("run meldet eine Konfigurationsdatei ohne Verzeichnis")
    void run_reportsConfigWithoutDirectory() {
        Path root = dir.getRoot();
        assertEquals(KilnMaster.EXIT_FAILURE, KilnMaster.run(new String[] {root.toString()}, errors, false));
        assertTrue(output().contains("master.config.no-directory"));
    }

    @Test
    @DisplayName("run lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void run_rejectsNull() {
        assertThrows(NullPointerException.class, () -> KilnMaster.run(null, errors, false));
        assertThrows(NullPointerException.class, () -> KilnMaster.run(new String[0], null, false));
    }
}
