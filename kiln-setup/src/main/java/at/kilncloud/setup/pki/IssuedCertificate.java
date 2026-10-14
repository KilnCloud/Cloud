package at.kilncloud.setup.pki;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * A certificate together with its private key: the identity of a master or node in mutual TLS.
 *
 * <p>This is a class and not a record on purpose: a record prints all components in {@code
 * toString()}, which would write the private key into logs (AGENTS.md §7.6). The key is never
 * printed by this class.
 */
public final class IssuedCertificate {
    private final X509Certificate certificate;
    private final PrivateKey privateKey;
    private final X509Certificate authority;

    /**
     * Creates an identity.
     *
     * @param certificate the certificate of this identity, signed by {@code authority}
     * @param privateKey the matching private key
     * @param authority the certificate of the issuing authority
     * @throws NullPointerException if an argument is {@code null}
     */
    IssuedCertificate(X509Certificate certificate, PrivateKey privateKey, X509Certificate authority) {
        this.certificate = Objects.requireNonNull(certificate, "pki.certificate.null");
        this.privateKey = Objects.requireNonNull(privateKey, "pki.key.null");
        this.authority = Objects.requireNonNull(authority, "pki.authority.null");
    }

    /**
     * Returns the certificate of this identity.
     *
     * @return the certificate
     */
    public X509Certificate certificate() {
        return certificate;
    }

    /**
     * Returns the private key. Treat it as a secret: never log it and write it only with
     * restricted file permissions.
     *
     * @return the private key
     */
    public PrivateKey privateKey() {
        return privateKey;
    }

    /**
     * Returns the certificate of the issuing authority, the trust anchor of this identity.
     *
     * @return the authority certificate
     */
    public X509Certificate authority() {
        return authority;
    }

    /**
     * Describes the identity without any key material.
     *
     * @return the subject of the certificate only
     */
    @Override
    public String toString() {
        return "IssuedCertificate[subject="
                + certificate.getSubjectX500Principal().getName() + ", privateKey=***]";
    }
}
