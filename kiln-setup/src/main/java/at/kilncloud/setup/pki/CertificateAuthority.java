package at.kilncloud.setup.pki;

import at.kilncloud.common.validation.NodeName;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * The certificate authority of one KilnCloud network.
 *
 * <p>It issues the short-lived certificates that master and nodes use for mutual TLS 1.3. All keys
 * are elliptic-curve keys (secp256r1) signed with SHA-256; serial numbers are random. The
 * authority certificate may only sign leaf certificates (path length 0). Leaf certificates are
 * short-lived and must be rotated before they expire, see {@link CertificateRotation}.
 *
 * <p>The authority key is a secret and is never exposed publicly. This class holds it in memory and
 * is a class, not a record, so that {@code toString()} cannot print it. Instances are immutable and
 * thread-safe.
 */
public final class CertificateAuthority {
    /** Longest lifetime of a leaf certificate, so a leaked key stops working within a year. */
    public static final Duration MAX_LEAF_VALIDITY = Duration.ofDays(366);

    /** Tolerance for clocks that differ slightly between machines. */
    private static final Duration CLOCK_SKEW = Duration.ofMinutes(5);

    private static final int MAX_NAMES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final X509Certificate certificate;
    private final PrivateKey privateKey;
    private final Clock clock;

    private CertificateAuthority(X509Certificate certificate, PrivateKey privateKey, Clock clock) {
        this.certificate = certificate;
        this.privateKey = privateKey;
        this.clock = clock;
    }

    /**
     * Creates a new authority with a fresh key pair and a self-signed certificate.
     *
     * <p>Runs on the calling thread; key generation takes a few milliseconds.
     *
     * @param commonName the name shown in the authority certificate, for example {@code KilnCloud CA}
     * @param validity how long the authority certificate is valid; must be positive
     * @param clock the time source, injected so tests are deterministic
     * @return a new authority
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code commonName} is blank or {@code validity} is not positive
     * @throws IllegalStateException if the platform cannot generate the keys or sign
     */
    public static CertificateAuthority create(String commonName, Duration validity, Clock clock) {
        Objects.requireNonNull(commonName, "pki.name.null");
        Objects.requireNonNull(validity, "pki.validity.null");
        Objects.requireNonNull(clock, "pki.clock.null");
        if (commonName.isBlank() || commonName.length() > 64) {
            throw new IllegalArgumentException("pki.name.invalid");
        }
        requirePositive(validity);
        try {
            KeyPair keys = newKeyPair();
            X500Name subject = name(commonName);
            Instant now = clock.instant();
            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    subject,
                    newSerial(),
                    Date.from(now.minus(CLOCK_SKEW)),
                    Date.from(now.plus(validity)),
                    subject,
                    keys.getPublic());
            JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            builder.addExtension(
                    Extension.subjectKeyIdentifier, false, utils.createSubjectKeyIdentifier(keys.getPublic()));
            X509Certificate certificate = sign(builder, keys.getPrivate());
            return new CertificateAuthority(certificate, keys.getPrivate(), clock);
        } catch (GeneralSecurityException | OperatorCreationException | java.io.IOException failure) {
            throw new IllegalStateException("pki.generation.failed", failure);
        }
    }

    /**
     * Restores an authority from stored material.
     *
     * @param certificate the authority certificate
     * @param privateKey the matching private key
     * @param clock the time source
     * @return the restored authority
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the certificate is not a CA certificate or the key does
     *     not belong to it
     */
    public static CertificateAuthority restore(X509Certificate certificate, PrivateKey privateKey, Clock clock) {
        Objects.requireNonNull(certificate, "pki.certificate.null");
        Objects.requireNonNull(privateKey, "pki.key.null");
        Objects.requireNonNull(clock, "pki.clock.null");
        if (certificate.getBasicConstraints() < 0) {
            throw new IllegalArgumentException("pki.authority.not-ca");
        }
        if (!keyMatches(certificate, privateKey)) {
            throw new IllegalArgumentException("pki.authority.key-mismatch");
        }
        return new CertificateAuthority(certificate, privateKey, clock);
    }

    /**
     * Returns the authority certificate: the trust anchor that master and nodes must trust.
     *
     * @return the self-signed certificate
     */
    public X509Certificate certificate() {
        return certificate;
    }

    /**
     * Returns the authority key for persistence inside this package only.
     *
     * @return the private key of the authority
     */
    PrivateKey privateKey() {
        return privateKey;
    }

    /**
     * Issues a client identity for a node.
     *
     * @param node the validated node name, used as the certificate common name
     * @param validity the lifetime; positive and at most {@link #MAX_LEAF_VALIDITY}
     * @return the new identity, usable to authenticate towards the master
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code validity} is out of range
     * @throws IllegalStateException if the platform cannot generate the keys or sign
     */
    public IssuedCertificate issueClient(NodeName node, Duration validity) {
        Objects.requireNonNull(node, "pki.node.null");
        return issue(node.value(), validity, KeyPurposeId.id_kp_clientAuth, List.of(), List.of());
    }

    /**
     * Issues a server identity for the master.
     *
     * <p>Clients verify that the host they connect to appears in the alternative names, so list every
     * name and address that nodes use to reach the master.
     *
     * @param name the common name of the server, validated like a node name
     * @param dnsNames DNS names of the server; hostname syntax is checked
     * @param addresses IP addresses of the server, built without a DNS lookup
     * @param validity the lifetime; positive and at most {@link #MAX_LEAF_VALIDITY}
     * @return the new identity, usable as the master's TLS server identity
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if no name or address is given, there are too many, a DNS
     *     name is malformed, or {@code validity} is out of range
     * @throws IllegalStateException if the platform cannot generate the keys or sign
     */
    public IssuedCertificate issueServer(
            NodeName name, List<String> dnsNames, List<InetAddress> addresses, Duration validity) {
        Objects.requireNonNull(name, "pki.node.null");
        List<String> dns = List.copyOf(Objects.requireNonNull(dnsNames, "pki.dns.null"));
        List<InetAddress> ips = List.copyOf(Objects.requireNonNull(addresses, "pki.addresses.null"));
        if ((dns.isEmpty() && ips.isEmpty()) || dns.size() + ips.size() > MAX_NAMES) {
            throw new IllegalArgumentException("pki.names.count");
        }
        for (String host : dns) {
            if (!DnsNames.isValid(host)) {
                throw new IllegalArgumentException("pki.dns.invalid");
            }
        }
        return issue(name.value(), validity, KeyPurposeId.id_kp_serverAuth, dns, ips);
    }

    private IssuedCertificate issue(
            String commonName,
            Duration validity,
            KeyPurposeId purpose,
            List<String> dnsNames,
            List<InetAddress> addresses) {
        Objects.requireNonNull(validity, "pki.validity.null");
        requirePositive(validity);
        if (validity.compareTo(MAX_LEAF_VALIDITY) > 0) {
            throw new IllegalArgumentException("pki.validity.too-long");
        }
        try {
            KeyPair keys = newKeyPair();
            Instant now = clock.instant();
            Instant notAfter = now.plus(validity);
            Instant authorityEnd = certificate.getNotAfter().toInstant();
            if (notAfter.isAfter(authorityEnd)) {
                notAfter = authorityEnd; // a leaf never outlives its authority
            }
            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded()),
                    newSerial(),
                    Date.from(now.minus(CLOCK_SKEW)),
                    Date.from(notAfter),
                    name(commonName),
                    keys.getPublic());
            JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            builder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(purpose));
            builder.addExtension(
                    Extension.subjectKeyIdentifier, false, utils.createSubjectKeyIdentifier(keys.getPublic()));
            builder.addExtension(
                    Extension.authorityKeyIdentifier, false, utils.createAuthorityKeyIdentifier(certificate));
            if (!dnsNames.isEmpty() || !addresses.isEmpty()) {
                builder.addExtension(Extension.subjectAlternativeName, false, alternativeNames(dnsNames, addresses));
            }
            X509Certificate leaf = sign(builder, privateKey);
            return new IssuedCertificate(leaf, keys.getPrivate(), certificate);
        } catch (GeneralSecurityException | OperatorCreationException | java.io.IOException failure) {
            throw new IllegalStateException("pki.generation.failed", failure);
        }
    }

    private static GeneralNames alternativeNames(List<String> dnsNames, List<InetAddress> addresses) {
        List<GeneralName> names = new ArrayList<>();
        for (String dns : dnsNames) {
            names.add(new GeneralName(GeneralName.dNSName, dns));
        }
        for (InetAddress address : addresses) {
            names.add(new GeneralName(GeneralName.iPAddress, address.getHostAddress()));
        }
        return new GeneralNames(names.toArray(new GeneralName[0]));
    }

    static boolean keyMatches(X509Certificate certificate, PrivateKey privateKey) {
        try {
            byte[] challenge = new byte[32];
            RANDOM.nextBytes(challenge);
            java.security.Signature signer = java.security.Signature.getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(challenge);
            byte[] signature = signer.sign();
            java.security.Signature verifier = java.security.Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(certificate.getPublicKey());
            verifier.update(challenge);
            return verifier.verify(signature);
        } catch (GeneralSecurityException failure) {
            return false;
        }
    }

    private static KeyPair newKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        return generator.generateKeyPair();
    }

    private static X509Certificate sign(X509v3CertificateBuilder builder, PrivateKey signingKey)
            throws OperatorCreationException, java.security.cert.CertificateException {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(signingKey);
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    private static X500Name name(String commonName) {
        return new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, commonName)
                .build();
    }

    private static BigInteger newSerial() {
        return new BigInteger(159, RANDOM).add(BigInteger.ONE);
    }

    private static void requirePositive(Duration validity) {
        if (validity.isZero() || validity.isNegative()) {
            throw new IllegalArgumentException("pki.validity.invalid");
        }
    }

    /**
     * Describes the authority without key material.
     *
     * @return the subject of the authority certificate only
     */
    @Override
    public String toString() {
        return "CertificateAuthority[subject="
                + certificate.getSubjectX500Principal().getName() + ", privateKey=***]";
    }
}
