package at.kilncloud.node.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the node daemon entry point. */
final class KilnNodeTest {
    /** Verifies that the M0 entry point terminates normally without external side effects. */
    @Test
    @DisplayName("main beendet das M0-Grundgerüst ohne Fehler")
    void main_returnsNormally() {
        assertDoesNotThrow(() -> KilnNode.main(new String[0]));
    }
}
