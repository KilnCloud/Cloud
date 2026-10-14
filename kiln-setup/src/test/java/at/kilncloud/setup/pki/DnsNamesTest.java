package at.kilncloud.setup.pki;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link DnsNames#isValid}. */
final class DnsNamesTest {
    @Test
    @DisplayName("isValid akzeptiert gewöhnliche Hostnamen")
    void isValid_acceptsOrdinaryNames() {
        assertTrue(DnsNames.isValid("localhost"));
        assertTrue(DnsNames.isValid("master.example.org"));
        assertTrue(DnsNames.isValid("a-b.c1.example"));
        assertTrue(DnsNames.isValid("A.B"));
    }

    @Test
    @DisplayName("isValid akzeptiert die Längengrenzen")
    void isValid_acceptsLengthBoundaries() {
        assertTrue(DnsNames.isValid("a".repeat(63)));
        String name253 = ("a".repeat(63) + ".").repeat(3) + "a".repeat(61);
        assertTrue(name253.length() == 253 && DnsNames.isValid(name253));
    }

    @Test
    @DisplayName("isValid lehnt leere und zu lange Namen ab")
    void isValid_rejectsEmptyAndTooLong() {
        assertFalse(DnsNames.isValid(null));
        assertFalse(DnsNames.isValid(""));
        assertFalse(DnsNames.isValid("a".repeat(64)));
        assertFalse(DnsNames.isValid(("a".repeat(63) + ".").repeat(3) + "a".repeat(62)));
    }

    @Test
    @DisplayName("isValid lehnt fehlerhafte Labels ab")
    void isValid_rejectsMalformedLabels() {
        assertFalse(DnsNames.isValid("."));
        assertFalse(DnsNames.isValid(".example"));
        assertFalse(DnsNames.isValid("example."));
        assertFalse(DnsNames.isValid("a..b"));
        assertFalse(DnsNames.isValid("-a.example"));
        assertFalse(DnsNames.isValid("a-.example"));
        assertFalse(DnsNames.isValid("a.-b"));
        assertFalse(DnsNames.isValid("a-"));
    }

    @Test
    @DisplayName("isValid lehnt unzulässige Zeichen ab")
    void isValid_rejectsIllegalCharacters() {
        assertFalse(DnsNames.isValid("a_b.example"));
        assertFalse(DnsNames.isValid("a b"));
        assertFalse(DnsNames.isValid("a*.example"));
        assertFalse(DnsNames.isValid("münchen.example"));
        assertFalse(DnsNames.isValid("a\nb"));
        assertFalse(DnsNames.isValid("a/b"));
    }
}
