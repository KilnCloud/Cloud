package at.kilncloud.setup.pki;

import at.kilncloud.common.validation.NodeName;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

/** Reads the identity out of a certificate that the TLS handshake has already verified. */
public final class CertificateNames {
    /** Prevents construction of this stateless utility class. */
    private CertificateNames() {}

    /**
     * Extracts the node name from the common name of a verified peer certificate.
     *
     * <p>Call this only after the TLS handshake has validated the certificate chain; the name itself
     * is validated again here, so a certificate with an unexpected common name yields no node.
     *
     * @param certificate the peer certificate
     * @return the node name, or empty if the certificate has no single valid node common name
     * @throws NullPointerException if {@code certificate} is {@code null}
     */
    public static Optional<NodeName> nodeNameOf(X509Certificate certificate) {
        Objects.requireNonNull(certificate, "pki.certificate.null");
        try {
            X500Name subject = new JcaX509CertificateHolder(certificate).getSubject();
            RDN[] commonNames = subject.getRDNs(BCStyle.CN);
            if (commonNames.length != 1) {
                return Optional.empty();
            }
            String value = IETFUtils.valueToString(commonNames[0].getFirst().getValue());
            return Optional.of(new NodeName(value));
        } catch (CertificateEncodingException | IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }
}
