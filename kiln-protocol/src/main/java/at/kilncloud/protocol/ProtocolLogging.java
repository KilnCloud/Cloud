package at.kilncloud.protocol;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TextFormat;
import java.util.Objects;

/**
 * Formats protocol messages for logs without leaking secrets.
 *
 * <p>The generated {@code toString()} prints every field, including the join token, because
 * Protobuf's default text format ignores {@code debug_redact}. Code that logs, traces or reports a
 * protocol message must use {@link #safeString(MessageOrBuilder)} instead (AGENTS.md §7.6).
 */
public final class ProtocolLogging {
    /** Prevents construction of this stateless utility class. */
    private ProtocolLogging() {}

    /**
     * Renders a message on one line with fields marked {@code debug_redact} replaced.
     *
     * @param message the message or builder to render
     * @return a single-line text that never contains the value of a redacted field
     * @throws NullPointerException if {@code message} is {@code null}
     */
    public static String safeString(MessageOrBuilder message) {
        Objects.requireNonNull(message, "protocol.message.null");
        return TextFormat.debugFormatPrinter().emittingSingleLine(true).printToString(message);
    }
}
