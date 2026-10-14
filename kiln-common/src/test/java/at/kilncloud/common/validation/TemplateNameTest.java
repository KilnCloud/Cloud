package at.kilncloud.common.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the security and boundary rules of {@link TemplateName}. */
final class TemplateNameTest {
    /** Verifies the ordinary allowlisted representation. */
    @Test
    @DisplayName("TemplateName akzeptiert einen erlaubten Namen")
    void constructor_acceptsAllowedName() {
        var name = new TemplateName("paper-1_21");

        assertEquals("paper-1_21", name.value());
    }

    /** Verifies both accepted length boundaries. */
    @Test
    @DisplayName("TemplateName akzeptiert beide Längengrenzen")
    void constructor_acceptsLengthBoundaries() {
        assertAll(
                () -> assertEquals("a", new TemplateName("a").value()),
                () -> assertEquals("a".repeat(64), new TemplateName("a".repeat(64)).value()));
    }

    /** Verifies that null cannot cross the validation boundary. */
    @Test
    @DisplayName("TemplateName lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void constructor_rejectsNull() {
        assertThrows(NullPointerException.class, () -> new TemplateName(null));
    }

    /** Verifies that empty and oversized values are rejected before regex evaluation. */
    @Test
    @DisplayName("TemplateName lehnt ungültige Längen ab")
    void constructor_rejectsInvalidLengths() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new TemplateName("")),
                () -> assertThrows(IllegalArgumentException.class, () -> new TemplateName("a".repeat(65))));
    }

    /** Verifies rejection of traversal, whitespace, Unicode, and log-injection payloads. */
    @Test
    @DisplayName("TemplateName lehnt nicht erlaubte und böswillige Zeichenfolgen ab")
    void constructor_rejectsDisallowedAndMaliciousInput() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new TemplateName("../template")),
                () -> assertThrows(IllegalArgumentException.class, () -> new TemplateName("template name")),
                () -> assertThrows(IllegalArgumentException.class, () -> new TemplateName("größer")),
                () -> assertThrows(IllegalArgumentException.class, () -> new TemplateName("template\rforged")));
    }
}
