package at.kilncloud.setup.pki;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

/**
 * Builds the TLS configuration for mutual TLS 1.3 between master and nodes.
 *
 * <p>Both sides trust only the KilnCloud authority, never the operating system's trust store, and
 * both present a certificate. Apply {@link #serverParameters()} to the server and {@link
 * #clientParameters()} to the client; they pin TLS 1.3, require a client certificate and make
 * the client verify the host name of the server.
 */
public final class TlsContexts {
    private static final String[] PROTOCOLS = {"TLSv1.3"};
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Prevents construction of this stateless utility class. */
    private TlsContexts() {}

    /**
     * Creates a TLS context that presents an identity and trusts only one authority.
     *
     * <p>The key store lives in memory only and is protected with a random password that is wiped
     * after use.
     *
     * @param identity the certificate and key presented to the peer
     * @param trustedAuthority the only authority whose certificates are accepted
     * @return an initialized TLS 1.3 context
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalStateException if the platform cannot build the context
     */
    public static SSLContext create(IssuedCertificate identity, X509Certificate trustedAuthority) {
        Objects.requireNonNull(identity, "pki.identity.null");
        Objects.requireNonNull(trustedAuthority, "pki.authority.null");
        char[] password = randomPassword();
        try {
            KeyStore identities = KeyStore.getInstance("PKCS12");
            identities.load(null, null);
            identities.setKeyEntry("identity", identity.privateKey(), password, new Certificate[] {
                identity.certificate(), identity.authority()
            });
            KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(identities, password);

            KeyStore trust = KeyStore.getInstance("PKCS12");
            trust.load(null, null);
            trust.setCertificateEntry("authority", trustedAuthority);
            TrustManagerFactory trusts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trusts.init(trust);

            SSLContext context = SSLContext.getInstance("TLSv1.3");
            context.init(keys.getKeyManagers(), trusts.getTrustManagers(), RANDOM);
            return context;
        } catch (GeneralSecurityException | java.io.IOException failure) {
            throw new IllegalStateException("pki.tls.failed", failure);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Returns the parameters a server socket or engine must use: TLS 1.3 only, and a client
     * certificate is required.
     *
     * @return new parameters, safe to modify by the caller
     */
    public static SSLParameters serverParameters() {
        SSLParameters parameters = new SSLParameters();
        parameters.setProtocols(PROTOCOLS.clone());
        parameters.setNeedClientAuth(true);
        return parameters;
    }

    /**
     * Returns the parameters a client socket or engine must use: TLS 1.3 only, and the server's
     * host name or address must match its certificate.
     *
     * @return new parameters, safe to modify by the caller
     */
    public static SSLParameters clientParameters() {
        SSLParameters parameters = new SSLParameters();
        parameters.setProtocols(PROTOCOLS.clone());
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        return parameters;
    }

    private static char[] randomPassword() {
        byte[] random = new byte[16];
        RANDOM.nextBytes(random);
        char[] password = HexFormat.of().formatHex(random).toCharArray();
        Arrays.fill(random, (byte) 0);
        return password;
    }
}
