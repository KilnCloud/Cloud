package at.kilncloud.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.protocol.v1.ProtocolVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link ProtocolVersions}. */
final class ProtocolVersionsTest {
    private static ProtocolVersion version(int major, int minor) {
        return ProtocolVersion.newBuilder().setMajor(major).setMinor(minor).build();
    }

    @Test
    @DisplayName("current liefert Version 1.0")
    void current_isVersionOnePointZero() {
        assertEquals(version(1, 0), ProtocolVersions.current());
    }

    @Test
    @DisplayName("isCompatible akzeptiert dieselbe Major-Version")
    void isCompatible_acceptsSameMajor() {
        assertTrue(ProtocolVersions.isCompatible(version(1, 0)));
    }

    @Test
    @DisplayName("isCompatible akzeptiert eine andere Minor-Version")
    void isCompatible_acceptsDifferentMinor() {
        assertTrue(ProtocolVersions.isCompatible(version(1, 7)));
    }

    @Test
    @DisplayName("isCompatible lehnt eine andere Major-Version ab")
    void isCompatible_rejectsDifferentMajor() {
        assertFalse(ProtocolVersions.isCompatible(version(2, 0)));
    }

    @Test
    @DisplayName("isCompatible lehnt eine fehlende Version (Major 0) ab")
    void isCompatible_rejectsUnsetVersion() {
        assertFalse(ProtocolVersions.isCompatible(ProtocolVersion.getDefaultInstance()));
    }

    @Test
    @DisplayName("isCompatible lehnt eine sehr große Major-Version (uint32) ab")
    void isCompatible_rejectsHugeMajor() {
        assertFalse(ProtocolVersions.isCompatible(version(-1, 0)));
    }

    @Test
    @DisplayName("isCompatible lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void isCompatible_rejectsNull() {
        assertThrows(NullPointerException.class, () -> ProtocolVersions.isCompatible(null));
    }
}
