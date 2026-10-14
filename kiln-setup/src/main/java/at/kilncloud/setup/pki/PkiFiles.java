package at.kilncloud.setup.pki;

import at.kilncloud.common.filesystem.SecureFiles;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * Stores and loads certificates and keys as PEM files.
 *
 * <p>Writes are atomic. Private keys are written with permissions {@code 600} by default, so only
 * the owner can read them (AGENTS.md §7.6). Reads are bounded and refuse symbolic links, because
 * these files come from disk and must not be trusted blindly. All methods block on file I/O on the
 * calling thread.
 */
public final class PkiFiles {
    private static final int MAX_PEM_BYTES = 64 * 1024;
    private static final String CERTIFICATE = "CERTIFICATE";
    private static final String PRIVATE_KEY = "PRIVATE KEY";

    /** Prevents construction of this stateless utility class. */
    private PkiFiles() {}

    /**
     * Writes a certificate as PEM.
     *
     * @param certificate the certificate to store
     * @param file the target file; replaced atomically
     * @throws NullPointerException if an argument is {@code null}
     * @throws IOException if the file cannot be written or the certificate cannot be encoded
     */
    public static void writeCertificate(X509Certificate certificate, Path file) throws IOException {
        Objects.requireNonNull(certificate, "pki.certificate.null");
        Objects.requireNonNull(file, "pki.file.null");
        try {
            SecureFiles.writeAtomically(file, pem(CERTIFICATE, certificate.getEncoded()), false);
        } catch (java.security.cert.CertificateEncodingException failure) {
            throw new IOException("pki.certificate.unencodable");
        }
    }

    /**
     * Writes a private key as PEM (PKCS#8).
     *
     * @param key the key to store; never logged by this method
     * @param file the target file; replaced atomically
     * @param restrictPermissions {@code true} to create the file with permissions {@code 600}, which
     *     needs a POSIX file system; {@code false} only for development on systems without POSIX
     *     permissions, where the caller must warn the operator
     * @throws NullPointerException if an argument is {@code null}
     * @throws UnsupportedOperationException if {@code restrictPermissions} is set but the file system
     *     has no POSIX permissions
     * @throws IOException if the file cannot be written
     */
    public static void writePrivateKey(PrivateKey key, Path file, boolean restrictPermissions) throws IOException {
        Objects.requireNonNull(key, "pki.key.null");
        Objects.requireNonNull(file, "pki.file.null");
        byte[] encoded = key.getEncoded();
        if (encoded == null) {
            throw new IOException("pki.key.unencodable");
        }
        byte[] pem = pem(PRIVATE_KEY, encoded);
        try {
            SecureFiles.writeAtomically(file, pem, restrictPermissions);
        } finally {
            Arrays.fill(encoded, (byte) 0);
            Arrays.fill(pem, (byte) 0);
        }
    }

    /**
     * Reads a certificate from a PEM file.
     *
     * @param file the file to read; must not be a symbolic link and at most 64 KiB
     * @return the certificate
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IOException if the file cannot be read or does not contain exactly a certificate
     */
    public static X509Certificate readCertificate(Path file) throws IOException {
        byte[] der = readPem(file, CERTIFICATE);
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(der));
        } catch (GeneralSecurityException | ClassCastException failure) {
            throw new IOException("pki.pem.invalid");
        }
    }

    /**
     * Reads an elliptic-curve private key from a PKCS#8 PEM file.
     *
     * @param file the file to read; must not be a symbolic link and at most 64 KiB
     * @return the private key
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IOException if the file cannot be read or does not contain exactly a private key
     */
    public static PrivateKey readPrivateKey(Path file) throws IOException {
        byte[] der = readPem(file, PRIVATE_KEY);
        try {
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException failure) {
            throw new IOException("pki.pem.invalid");
        } finally {
            Arrays.fill(der, (byte) 0);
        }
    }

    /**
     * Stores an authority: its certificate and, with restricted permissions, its private key.
     *
     * @param authority the authority to store
     * @param certificateFile target for the authority certificate
     * @param keyFile target for the authority private key
     * @param restrictPermissions see {@link #writePrivateKey(PrivateKey, Path, boolean)}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IOException if a file cannot be written
     */
    public static void writeAuthority(
            CertificateAuthority authority, Path certificateFile, Path keyFile, boolean restrictPermissions)
            throws IOException {
        Objects.requireNonNull(authority, "pki.authority.null");
        writePrivateKey(authority.privateKey(), keyFile, restrictPermissions);
        writeCertificate(authority.certificate(), certificateFile);
    }

    /**
     * Loads an authority from its two files.
     *
     * @param certificateFile the authority certificate
     * @param keyFile the authority private key
     * @param clock the time source of the restored authority
     * @return the authority
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the key does not belong to the certificate or the
     *     certificate is not a CA certificate
     * @throws IOException if a file cannot be read or parsed
     */
    public static CertificateAuthority readAuthority(Path certificateFile, Path keyFile, Clock clock)
            throws IOException {
        return CertificateAuthority.restore(readCertificate(certificateFile), readPrivateKey(keyFile), clock);
    }

    private static byte[] pem(String type, byte[] der) throws IOException {
        StringWriter text = new StringWriter();
        try (PemWriter writer = new PemWriter(text)) {
            writer.writeObject(new PemObject(type, der));
        }
        return text.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] readPem(Path file, String expectedType) throws IOException {
        Objects.requireNonNull(file, "pki.file.null");
        String text = new String(SecureFiles.readBounded(file, MAX_PEM_BYTES), StandardCharsets.US_ASCII);
        try (PemReader reader = new PemReader(new StringReader(text))) {
            PemObject object = reader.readPemObject();
            if (object == null || !expectedType.equals(object.getType()) || reader.readPemObject() != null) {
                throw new IOException("pki.pem.invalid");
            }
            return object.getContent();
        }
    }
}
