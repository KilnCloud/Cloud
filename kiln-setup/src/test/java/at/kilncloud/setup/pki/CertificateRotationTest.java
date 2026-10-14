package at.kilncloud.setup.pki;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.setup.TestClock;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link CertificateRotation#isRotationDue}. */
final class CertificateRotationTest {
    private static final Instant START = Instant.parse("2026-10-14T12:00:00Z");
    private final TestClock clock = new TestClock(START);
    private final X509Certificate leaf = CertificateAuthority.create("KilnCloud CA", Duration.ofDays(365), clock)
            .issueClient(new NodeName("node-1"), Duration.ofDays(30))
            .certificate();

    @Test
    @DisplayName("isRotationDue ist falsch, solange genug Gültigkeit übrig ist")
    void isRotationDue_falseWhenPlentyOfValidityLeft() {
        assertFalse(CertificateRotation.isRotationDue(leaf, Duration.ofDays(7), clock));
    }

    @Test
    @DisplayName("isRotationDue ist wahr innerhalb der Vorlaufzeit vor dem Ablauf")
    void isRotationDue_trueWithinLeadTime() {
        clock.advance(Duration.ofDays(24));
        assertTrue(CertificateRotation.isRotationDue(leaf, Duration.ofDays(7), clock));
    }

    @Test
    @DisplayName("isRotationDue ist wahr genau an der Grenze der Vorlaufzeit")
    void isRotationDue_trueAtLeadTimeBoundary() {
        clock.advance(Duration.ofDays(23));
        assertTrue(CertificateRotation.isRotationDue(leaf, Duration.ofDays(7), clock));
    }

    @Test
    @DisplayName("isRotationDue ist wahr für ein abgelaufenes Zertifikat")
    void isRotationDue_trueWhenExpired() {
        clock.advance(Duration.ofDays(31));
        assertTrue(CertificateRotation.isRotationDue(leaf, Duration.ZERO, clock));
    }

    @Test
    @DisplayName("isRotationDue ist wahr für ein noch nicht gültiges Zertifikat")
    void isRotationDue_trueWhenNotYetValid() {
        TestClock early = new TestClock(START.minus(Duration.ofDays(1)));
        assertTrue(CertificateRotation.isRotationDue(leaf, Duration.ZERO, early));
    }

    @Test
    @DisplayName("isRotationDue lehnt ungültige Argumente ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void isRotationDue_rejectsInvalidArguments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CertificateRotation.isRotationDue(leaf, Duration.ofDays(-1), clock));
        assertThrows(NullPointerException.class, () -> CertificateRotation.isRotationDue(null, Duration.ZERO, clock));
        assertThrows(NullPointerException.class, () -> CertificateRotation.isRotationDue(leaf, null, clock));
        assertThrows(NullPointerException.class, () -> CertificateRotation.isRotationDue(leaf, Duration.ZERO, null));
    }
}
