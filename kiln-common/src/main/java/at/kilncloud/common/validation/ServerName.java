package at.kilncloud.common.validation;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A validated server name used by KilnCloud process modules.
 *
 * <p>Names contain between one and 64 ASCII letters, digits, underscores, or hyphens and must
 * start with an ASCII letter or digit. Validation bounds the input length before applying a
 * linear-time allowlist expression.
 *
 * @param value the validated server name
 */
public record ServerName(String value) {
    /** Maximum number of characters accepted for a server name. */
    private static final int MAX_LENGTH = 64;

    /** Linear-time allowlist for a bounded server name. */
    private static final Pattern ALLOWED_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$");

    /**
     * Creates a validated server name.
     *
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} violates the documented name rules
     */
    public ServerName {
        Objects.requireNonNull(value, "validation.server-name.null");
        if (value.isEmpty()
                || value.length() > MAX_LENGTH
                || !ALLOWED_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException("validation.server-name.invalid");
        }
    }
}
