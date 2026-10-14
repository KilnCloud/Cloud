package at.kilncloud.common.validation;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A validated node name used in certificates, join tokens and the node registry.
 *
 * <p>Names contain between one and 64 ASCII letters, digits, underscores, or hyphens and must
 * start with an ASCII letter or digit. Validation bounds the input length before applying a
 * linear-time allowlist expression. The strict charset keeps the name safe as a certificate common
 * name and as a file name.
 *
 * @param value the validated node name
 */
public record NodeName(String value) {
    /** Maximum number of characters accepted for a node name. */
    private static final int MAX_LENGTH = 64;

    /** Linear-time allowlist for a bounded node name. */
    private static final Pattern ALLOWED_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$");

    /**
     * Creates a validated node name.
     *
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} violates the documented name rules
     */
    public NodeName {
        Objects.requireNonNull(value, "validation.node-name.null");
        if (value.isEmpty()
                || value.length() > MAX_LENGTH
                || !ALLOWED_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException("validation.node-name.invalid");
        }
    }
}
