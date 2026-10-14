package at.kilncloud.common.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the security and boundary rules of {@link ServerName}. */
final class ServerNameTest {
    /** Verifies the ordinary allowlisted representation. */
    @Test
    @DisplayName("ServerName akzeptiert einen erlaubten Namen")
    void constructor_acceptsAllowedName() {
        var name = new ServerName("lobby-01_eu");

        assertEquals("lobby-01_eu", name.value());
    }

    /** Verifies both accepted length boundaries. */
    @Test
    @DisplayName("ServerName akzeptiert beide Längengrenzen")
    void constructor_acceptsLengthBoundaries() {
        assertAll(
                () -> assertEquals("a", new ServerName("a").value()),
                () -> assertEquals("a".repeat(64), new ServerName("a".repeat(64)).value()));
    }

    /** Verifies that null cannot cross the validation boundary. */
    @Test
    @DisplayName("ServerName lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void constructor_rejectsNull() {
        assertThrows(NullPointerException.class, () -> new ServerName(null));
    }

    /** Verifies that empty and oversized values are rejected before regex evaluation. */
    @Test
    @DisplayName("ServerName lehnt ungültige Längen ab")
    void constructor_rejectsInvalidLengths() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new ServerName("")),
                () -> assertThrows(IllegalArgumentException.class, () -> new ServerName("a".repeat(65))));
    }

    /** Verifies rejection of traversal, whitespace, Unicode, and log-injection payloads. */
    @Test
    @DisplayName("ServerName lehnt nicht erlaubte und böswillige Zeichenfolgen ab")
    void constructor_rejectsDisallowedAndMaliciousInput() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new ServerName("../master")),
                () -> assertThrows(IllegalArgumentException.class, () -> new ServerName("server name")),
                () -> assertThrows(IllegalArgumentException.class, () -> new ServerName("überall")),
                () -> assertThrows(IllegalArgumentException.class, () -> new ServerName("server\nforged")));
    }
}
