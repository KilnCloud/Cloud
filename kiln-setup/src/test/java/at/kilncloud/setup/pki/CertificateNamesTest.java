package at.kilncloud.setup.pki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.setup.TestClock;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link CertificateNames#nodeNameOf}. */
final class CertificateNamesTest {
    private static final Instant START = Instant.parse("2026-10-14T12:00:00Z");
    private final CertificateAuthority authority =
            CertificateAuthority.create("KilnCloud CA", Duration.ofDays(365), new TestClock(START));

    private static X509Certificate certificateWithSubject(String subject) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair keys = generator.generateKeyPair();
        X500Name name = new X500Name(subject);
        var builder = new JcaX509v3CertificateBuilder(
                name,
                BigInteger.ONE,
                Date.from(START),
                Date.from(START.plus(Duration.ofDays(1))),
                name,
                keys.getPublic());
        return new JcaX509CertificateConverter()
                .getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keys.getPrivate())));
    }

    @Test
    @DisplayName("nodeNameOf liest den Namen aus einem ausgestellten Zertifikat")
    void nodeNameOf_readsNameFromIssuedCertificate() {
        X509Certificate leaf = authority
                .issueClient(new NodeName("node-7"), Duration.ofDays(1))
                .certificate();
        assertEquals(Optional.of(new NodeName("node-7")), CertificateNames.nodeNameOf(leaf));
    }

    @Test
    @DisplayName("nodeNameOf liefert nichts für einen ungültigen Common Name")
    void nodeNameOf_emptyForInvalidCommonName() {
        assertTrue(CertificateNames.nodeNameOf(authority.certificate()).isEmpty(), "CA name contains a space");
    }

    @Test
    @DisplayName("nodeNameOf liefert nichts, wenn der Common Name fehlt")
    void nodeNameOf_emptyWithoutCommonName() throws Exception {
        assertTrue(
                CertificateNames.nodeNameOf(certificateWithSubject("O=Example")).isEmpty());
    }

    @Test
    @DisplayName("nodeNameOf liefert nichts bei mehreren Common Names")
    void nodeNameOf_emptyWithSeveralCommonNames() throws Exception {
        assertTrue(
                CertificateNames.nodeNameOf(certificateWithSubject("CN=a,CN=b")).isEmpty());
    }

    @Test
    @DisplayName("nodeNameOf lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void nodeNameOf_rejectsNull() {
        assertThrows(NullPointerException.class, () -> CertificateNames.nodeNameOf(null));
    }
}
