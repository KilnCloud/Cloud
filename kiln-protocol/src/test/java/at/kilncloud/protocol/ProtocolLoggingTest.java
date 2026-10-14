package at.kilncloud.protocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.protocol.v1.RegisterRequest;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link ProtocolLogging}. */
final class ProtocolLoggingTest {
    private static final String SECRET = "geheimer-join-token-1234567890";

    private static RegisterRequest.Builder request() {
        return RegisterRequest.newBuilder()
                .setNodeName("node-1")
                .setJoinToken(ByteString.copyFrom(SECRET, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("safeString verbirgt den Join-Token einer Nachricht")
    void safeString_redactsJoinTokenOfMessage() {
        String text = ProtocolLogging.safeString(request().build());
        assertFalse(text.contains(SECRET), text);
        assertTrue(text.contains("node-1"), text);
    }

    @Test
    @DisplayName("safeString verbirgt den Join-Token auch bei einem Builder")
    void safeString_redactsJoinTokenOfBuilder() {
        assertFalse(ProtocolLogging.safeString(request()).contains(SECRET));
    }

    @Test
    @DisplayName("safeString lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void safeString_rejectsNull() {
        assertThrows(NullPointerException.class, () -> ProtocolLogging.safeString(null));
    }
}
