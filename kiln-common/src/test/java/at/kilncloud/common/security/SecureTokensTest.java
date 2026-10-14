package at.kilncloud.common.security;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests token allocation boundaries and secret comparisons in {@link SecureTokens}. */
final class SecureTokensTest {
    /** Verifies a normal token request returns the requested number of bytes. */
    @Test
    @DisplayName("generate erzeugt ein Token in der angeforderten Länge")
    void generate_returnsRequestedLength() {
        byte[] token = SecureTokens.generate(48);

        assertEquals(48, token.length);
    }

    /** Verifies both permitted token-size boundaries. */
    @Test
    @DisplayName("generate akzeptiert beide Längengrenzen")
    void generate_acceptsLengthBoundaries() {
        assertAll(
                () -> assertEquals(32, SecureTokens.generate(32).length),
                () -> assertEquals(1024, SecureTokens.generate(1024).length));
    }

    /** Verifies undersized and excessive allocations are rejected. */
    @Test
    @DisplayName("generate lehnt unsichere oder übermäßige Längen ab")
    void generate_rejectsInvalidLengths() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> SecureTokens.generate(-1)),
                () -> assertThrows(IllegalArgumentException.class, () -> SecureTokens.generate(31)),
                () -> assertThrows(IllegalArgumentException.class, () -> SecureTokens.generate(1025)));
    }

    /** Verifies each call allocates independently owned secret storage. */
    @Test
    @DisplayName("generate gibt bei jedem Aufruf ein neues Array zurück")
    void generate_returnsIndependentArrays() {
        assertNotSame(SecureTokens.generate(32), SecureTokens.generate(32));
    }

    /** Verifies equal secret values compare successfully. */
    @Test
    @DisplayName("constantTimeEquals erkennt gleiche Bytefolgen")
    void constantTimeEquals_returnsTrueForEqualValues() {
        assertTrue(SecureTokens.constantTimeEquals(new byte[] {1, 2, 3}, new byte[] {1, 2, 3}));
    }

    /** Verifies content and length differences compare unsuccessfully. */
    @Test
    @DisplayName("constantTimeEquals erkennt unterschiedliche Bytefolgen")
    void constantTimeEquals_returnsFalseForDifferentValues() {
        assertAll(
                () -> assertFalse(SecureTokens.constantTimeEquals(new byte[] {1, 2, 3}, new byte[] {1, 2, 4})),
                () -> assertFalse(SecureTokens.constantTimeEquals(new byte[] {1, 2, 3}, new byte[] {1, 2})));
    }

    /** Verifies null cannot cross either comparison boundary. */
    @Test
    @DisplayName("constantTimeEquals lehnt null für beide Parameter ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void constantTimeEquals_rejectsNullInputs() {
        byte[] value = {1};
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> SecureTokens.constantTimeEquals(null, value)),
                () -> assertThrows(NullPointerException.class, () -> SecureTokens.constantTimeEquals(value, null)));
    }
}
