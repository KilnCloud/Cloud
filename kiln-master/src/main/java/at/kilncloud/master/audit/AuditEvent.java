package at.kilncloud.master.audit;

import java.time.Instant;
import java.util.Objects;

/**
 * One security-relevant action: who did what to which target, when, with which outcome (AGENTS.md
 * §7.3).
 *
 * <p>The texts are short machine-readable keys such as {@code node.register} and {@code denied}.
 * They never contain secrets, tokens or request bodies.
 *
 * @param time when the action happened
 * @param actor who acted, for example a node name; not blank, at most 256 characters
 * @param action what was attempted, for example {@code node.register}; not blank, at most 256 characters
 * @param target what the action was applied to, for example a node name; not blank, at most 256
 *     characters (use {@code -} if there is none)
 * @param outcome the result, for example {@code accepted} or {@code denied:name-mismatch}; not blank,
 *     at most 256 characters
 */
public record AuditEvent(Instant time, String actor, String action, String target, String outcome) {
    /** Longest accepted text, so a hostile peer cannot bloat the log. */
    static final int MAX_TEXT = 256;

    /**
     * Creates an audit event.
     *
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a text is blank or longer than 256 characters
     */
    public AuditEvent {
        Objects.requireNonNull(time, "audit.time.null");
        requireText(actor, "audit.actor.invalid");
        requireText(action, "audit.action.invalid");
        requireText(target, "audit.target.invalid");
        requireText(outcome, "audit.outcome.invalid");
    }

    private static void requireText(String text, String key) {
        Objects.requireNonNull(text, key);
        if (text.isBlank() || text.length() > MAX_TEXT) {
            throw new IllegalArgumentException(key);
        }
    }
}
