package at.kilncloud.setup.pki;

import org.jspecify.annotations.Nullable;

/**
 * Validates DNS host names without regular expressions, so hostile input cannot cause
 * catastrophic backtracking.
 */
final class DnsNames {
    private static final int MAX_LENGTH = 253;
    private static final int MAX_LABEL_LENGTH = 63;

    /** Prevents construction of this stateless utility class. */
    private DnsNames() {}

    /**
     * Checks a host name against RFC 1123 syntax: dot-separated labels of letters, digits and
     * hyphens, each 1 to 63 characters, not starting or ending with a hyphen, 253 characters in total.
     *
     * @param name the candidate host name; {@code null} is invalid
     * @return {@code true} if the name is a syntactically valid host name
     */
    static boolean isValid(@Nullable String name) {
        if (name == null || name.isEmpty() || name.length() > MAX_LENGTH) {
            return false;
        }
        int labelLength = 0;
        char previous = '.';
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.') {
                if (labelLength == 0 || previous == '-') {
                    return false;
                }
                labelLength = 0;
            } else if (isAllowed(c)) {
                if (labelLength == 0 && c == '-') {
                    return false;
                }
                labelLength++;
                if (labelLength > MAX_LABEL_LENGTH) {
                    return false;
                }
            } else {
                return false;
            }
            previous = c;
        }
        return labelLength > 0 && previous != '-';
    }

    private static boolean isAllowed(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-';
    }
}
