package at.kilncloud.setup.pki;

import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Decides when a certificate must be replaced. */
public final class CertificateRotation {
    /** Prevents construction of this stateless utility class. */
    private CertificateRotation() {}

    /**
     * Tells whether a certificate is expired or will expire within a lead time.
     *
     * <p>Rotate early, not at the last second: a node that renews with a margin keeps its
     * connection while the new certificate is rolled out.
     *
     * @param certificate the certificate to check
     * @param leadTime how long before expiry rotation is due; must not be negative
     * @param clock the time source
     * @return {@code true} if the certificate expires at or before {@code now + leadTime}, or is
     *     not yet valid
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code leadTime} is negative
     */
    public static boolean isRotationDue(X509Certificate certificate, Duration leadTime, Clock clock) {
        Objects.requireNonNull(certificate, "pki.certificate.null");
        Objects.requireNonNull(leadTime, "pki.lead-time.null");
        Objects.requireNonNull(clock, "pki.clock.null");
        if (leadTime.isNegative()) {
            throw new IllegalArgumentException("pki.lead-time.invalid");
        }
        Instant now = clock.instant();
        boolean notYetValid = now.isBefore(certificate.getNotBefore().toInstant());
        boolean expiresSoon =
                !now.plus(leadTime).isBefore(certificate.getNotAfter().toInstant());
        return notYetValid || expiresSoon;
    }
}
