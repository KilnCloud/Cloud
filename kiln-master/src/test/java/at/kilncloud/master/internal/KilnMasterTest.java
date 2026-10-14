package at.kilncloud.master.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the master process entry point. */
final class KilnMasterTest {
    /** Verifies that the M0 entry point terminates normally without external side effects. */
    @Test
    @DisplayName("main beendet das M0-Grundgerüst ohne Fehler")
    void main_returnsNormally() {
        assertDoesNotThrow(() -> KilnMaster.main(new String[0]));
    }
}
