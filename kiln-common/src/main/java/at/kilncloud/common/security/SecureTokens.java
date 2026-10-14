package at.kilncloud.common.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Objects;

/** Generates high-entropy token bytes and compares secret byte sequences in constant time. */
public final class SecureTokens {
    /** Minimum token size that provides a 256-bit random value. */
    private static final int MINIMUM_TOKEN_BYTES = 32;

    /** Defensive allocation limit for a single generated token. */
    private static final int MAXIMUM_TOKEN_BYTES = 1024;

    /** Process-wide cryptographically secure random source; {@link SecureRandom} is thread-safe. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Prevents construction of this stateless utility class. */
    private SecureTokens() {}

    /**
     * Generates a fresh cryptographically secure token.
     *
     * <p>The returned mutable array belongs to the caller and should be overwritten after use when
     * practical.
     *
     * @param byteLength requested token length from 32 through 1024 bytes
     * @return a newly allocated token filled by {@link SecureRandom}
     * @throws IllegalArgumentException if {@code byteLength} is outside the allowed range
     */
    public static byte[] generate(int byteLength) {
        if (byteLength < MINIMUM_TOKEN_BYTES || byteLength > MAXIMUM_TOKEN_BYTES) {
            throw new IllegalArgumentException("security.token.length.invalid");
        }
        byte[] token = new byte[byteLength];
        RANDOM.nextBytes(token);
        return token;
    }

    /**
     * Compares two secret byte sequences using the JDK constant-time comparison primitive.
     *
     * @param expected expected secret bytes
     * @param actual supplied secret bytes
     * @return {@code true} only when both sequences contain the same bytes
     * @throws NullPointerException if either argument is {@code null}
     */
    public static boolean constantTimeEquals(byte[] expected, byte[] actual) {
        Objects.requireNonNull(expected, "security.expected.null");
        Objects.requireNonNull(actual, "security.actual.null");
        return MessageDigest.isEqual(expected, actual);
    }
}
