package at.kilncloud.common.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the security and boundary rules of {@link NodeName}. */
final class NodeNameTest {
    /** Verifies the ordinary allowlisted representation. */
    @Test
    @DisplayName("NodeName akzeptiert einen erlaubten Namen")
    void constructor_acceptsAllowedName() {
        assertEquals("node-01_eu", new NodeName("node-01_eu").value());
    }

    /** Verifies both accepted length boundaries. */
    @Test
    @DisplayName("NodeName akzeptiert beide Längengrenzen")
    void constructor_acceptsLengthBoundaries() {
        assertAll(
                () -> assertEquals("a", new NodeName("a").value()),
                () -> assertEquals("a".repeat(64), new NodeName("a".repeat(64)).value()));
    }

    /** Verifies that null cannot cross the validation boundary. */
    @Test
    @DisplayName("NodeName lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void constructor_rejectsNull() {
        assertThrows(NullPointerException.class, () -> new NodeName(null));
    }

    /** Verifies that empty and oversized values are rejected. */
    @Test
    @DisplayName("NodeName lehnt leere und zu lange Namen ab")
    void constructor_rejectsEmptyAndOversizedNames() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new NodeName("")),
                () -> assertThrows(IllegalArgumentException.class, () -> new NodeName("a".repeat(65))));
    }

    /** Verifies that characters that are dangerous in certificates, paths and logs are rejected. */
    @Test
    @DisplayName("NodeName lehnt gefährliche Zeichen ab")
    void constructor_rejectsDangerousCharacters() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new NodeName("../etc")),
                () -> assertThrows(IllegalArgumentException.class, () -> new NodeName("a,CN=admin")),
                () -> assertThrows(IllegalArgumentException.class, () -> new NodeName("a b")),
                () -> assertThrows(IllegalArgumentException.class, () -> new NodeName("-a")),
                () -> assertThrows(IllegalArgumentException.class, () -> new NodeName("a\nb")),
                () -> assertThrows(IllegalArgumentException.class, () -> new NodeName("knöten")));
    }
}
