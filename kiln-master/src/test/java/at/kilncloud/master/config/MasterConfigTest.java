package at.kilncloud.master.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the validation of {@link MasterConfig}. */
final class MasterConfigTest {
    private static MasterConfig config(String host, int port, String cert, int timeout) {
        return new MasterConfig(host, port, cert, "master.key", "ca.crt", "audit.log", timeout, false);
    }

    @Test
    @DisplayName("Der Konstruktor akzeptiert eine gültige Konfiguration")
    void constructor_acceptsValidConfiguration() {
        MasterConfig config = config("0.0.0.0", 25_570, "master.crt", 30);
        assertEquals(25_570, config.port());
        assertFalse(config.allowUnrestrictedFilePermissions());
    }

    @Test
    @DisplayName("Der Konstruktor akzeptiert die Grenzen von Port und Timeout")
    void constructor_acceptsBoundaries() {
        assertEquals(1, config("h", 1, "c", 5).port());
        assertEquals(65_535, config("h", 65_535, "c", 3600).port());
    }

    @Test
    @DisplayName("Der Konstruktor lehnt Ports und Timeouts außerhalb des Bereichs ab")
    void constructor_rejectsOutOfRangeNumbers() {
        assertThrows(IllegalArgumentException.class, () -> config("h", 0, "c", 30));
        assertThrows(IllegalArgumentException.class, () -> config("h", 65_536, "c", 30));
        assertThrows(IllegalArgumentException.class, () -> config("h", 1, "c", 4));
        assertThrows(IllegalArgumentException.class, () -> config("h", 1, "c", 3601));
    }

    @Test
    @DisplayName("Der Konstruktor lehnt leere, zu lange und ungültige Texte ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void constructor_rejectsInvalidTexts() {
        assertThrows(IllegalArgumentException.class, () -> config(" ", 1, "c", 30));
        assertThrows(IllegalArgumentException.class, () -> config("h", 1, "", 30));
        assertThrows(IllegalArgumentException.class, () -> config("h", 1, "x".repeat(4097), 30));
        assertThrows(IllegalArgumentException.class, () -> config("h", 1, "bad\u0000name", 30));
        assertThrows(NullPointerException.class, () -> config(null, 1, "c", 30));
        assertThrows(NullPointerException.class, () -> config("h", 1, null, 30));
    }

    @Test
    @DisplayName("resolve macht relative Dateinamen absolut und lässt absolute unverändert")
    void resolve_relativeAndAbsolute() {
        Path base = Path.of("config").toAbsolutePath();
        assertEquals(base.resolve("master.crt").normalize(), MasterConfig.resolve(base, "master.crt"));
        assertEquals(base.resolve("sub/a.log").normalize(), MasterConfig.resolve(base, "sub/../sub/a.log"));
        Path absolute = Path.of("absolute").toAbsolutePath().resolve("x.log");
        assertEquals(absolute, MasterConfig.resolve(base, absolute.toString()));
        assertTrue(MasterConfig.resolve(base, "a").isAbsolute());
    }

    @Test
    @DisplayName("resolve lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void resolve_rejectsNull() {
        assertThrows(NullPointerException.class, () -> MasterConfig.resolve(null, "a"));
        assertThrows(NullPointerException.class, () -> MasterConfig.resolve(Path.of("."), null));
    }
}
