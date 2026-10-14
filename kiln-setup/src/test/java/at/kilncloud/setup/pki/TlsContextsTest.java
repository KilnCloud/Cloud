package at.kilncloud.setup.pki;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.setup.TestClock;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the issued certificates work for real mutual TLS 1.3 handshakes over the loopback
 * interface: a valid pair connects, and every kind of invalid identity is refused.
 */
final class TlsContextsTest {
    private static final int TIMEOUT_MS = 10_000;
    private static final byte[] PING = {1, 2, 3};

    private final TestClock clock = new TestClock(Instant.now());
    private final CertificateAuthority authority =
            CertificateAuthority.create("KilnCloud CA", Duration.ofDays(30), clock);
    private final InetAddress loopback = InetAddress.getLoopbackAddress();

    private IssuedCertificate master() throws Exception {
        return authority.issueServer(
                new NodeName("master"),
                List.of("localhost"),
                List.of(InetAddress.getByAddress(new byte[] {127, 0, 0, 1})),
                Duration.ofDays(1));
    }

    private IssuedCertificate node(String name) {
        return authority.issueClient(new NodeName(name), Duration.ofDays(1));
    }

    /** Outcome of one connection attempt: what the server saw, or why the exchange failed. */
    private record Result(Optional<X509Certificate> serverSawClient, Optional<Exception> failure) {}

    /**
     * Runs a server and a client against each other and exchanges three bytes. The exchange (not
     * only the handshake) is what proves acceptance, because TLS 1.3 reports a rejected client
     * certificate only after the client has finished its side of the handshake.
     */
    private Result connect(SSLContext serverContext, SSLContext clientContext) throws Exception {
        try (SSLServerSocket server =
                (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0, 1, loopback)) {
            server.setSSLParameters(TlsContexts.serverParameters());
            server.setSoTimeout(TIMEOUT_MS);
            CompletableFuture<Optional<X509Certificate>> serverSide = CompletableFuture.supplyAsync(() -> {
                try (SSLSocket socket = (SSLSocket) server.accept()) {
                    socket.setSoTimeout(TIMEOUT_MS);
                    InputStream in = socket.getInputStream();
                    byte[] received = in.readNBytes(PING.length);
                    assertArrayEquals(PING, received);
                    X509Certificate peer = (X509Certificate) socket.getSession().getPeerCertificates()[0];
                    OutputStream out = socket.getOutputStream();
                    out.write(PING);
                    out.flush();
                    return Optional.of(peer);
                } catch (IOException failure) {
                    return Optional.empty();
                }
            });
            Exception clientFailure = null;
            try (SSLSocket client =
                    (SSLSocket) clientContext.getSocketFactory().createSocket(loopback, server.getLocalPort())) {
                SSLParameters parameters = TlsContexts.clientParameters();
                client.setSSLParameters(parameters);
                client.setSoTimeout(TIMEOUT_MS);
                OutputStream out = client.getOutputStream();
                out.write(PING);
                out.flush();
                byte[] answer = client.getInputStream().readNBytes(PING.length);
                if (answer.length != PING.length) {
                    clientFailure = new IOException("no answer");
                }
            } catch (IOException failure) {
                clientFailure = failure;
            }
            try {
                return new Result(
                        serverSide.get(TIMEOUT_MS, TimeUnit.MILLISECONDS), Optional.ofNullable(clientFailure));
            } catch (ExecutionException | TimeoutException failure) {
                return new Result(Optional.empty(), Optional.of(clientFailure != null ? clientFailure : failure));
            }
        }
    }

    @Test
    @DisplayName("create ermöglicht einen gegenseitig authentifizierten TLS-1.3-Aufbau")
    void create_allowsMutualTls() throws Exception {
        Result result = connect(
                TlsContexts.create(master(), authority.certificate()),
                TlsContexts.create(node("node-1"), authority.certificate()));
        assertTrue(result.failure().isEmpty(), String.valueOf(result.failure()));
        assertEquals(
                Optional.of(new NodeName("node-1")),
                CertificateNames.nodeNameOf(result.serverSawClient().orElseThrow()));
    }

    @Test
    @DisplayName("Der Server weist einen Client mit Zertifikat einer fremden CA ab")
    void server_rejectsClientFromOtherAuthority() throws Exception {
        CertificateAuthority other = CertificateAuthority.create("Evil CA", Duration.ofDays(30), clock);
        IssuedCertificate intruder = other.issueClient(new NodeName("node-1"), Duration.ofDays(1));
        Result result = connect(
                TlsContexts.create(master(), authority.certificate()),
                TlsContexts.create(intruder, other.certificate()));
        assertTrue(result.serverSawClient().isEmpty());
    }

    @Test
    @DisplayName("Der Client weist einen Server mit Zertifikat einer fremden CA ab")
    void client_rejectsServerFromOtherAuthority() throws Exception {
        CertificateAuthority other = CertificateAuthority.create("Evil CA", Duration.ofDays(30), clock);
        IssuedCertificate fakeMaster = other.issueServer(
                new NodeName("master"),
                List.of("localhost"),
                List.of(InetAddress.getByAddress(new byte[] {127, 0, 0, 1})),
                Duration.ofDays(1));
        Result result = connect(
                TlsContexts.create(fakeMaster, other.certificate()),
                TlsContexts.create(node("node-1"), authority.certificate()));
        assertTrue(result.failure().isPresent());
        assertTrue(result.serverSawClient().isEmpty());
    }

    @Test
    @DisplayName("Der Server weist ein abgelaufenes Client-Zertifikat ab")
    void server_rejectsExpiredClientCertificate() throws Exception {
        TestClock past = new TestClock(Instant.now().minus(Duration.ofDays(10)));
        CertificateAuthority sameKeyAuthority =
                CertificateAuthority.restore(authority.certificate(), authority.privateKey(), past);
        IssuedCertificate expired = sameKeyAuthority.issueClient(new NodeName("node-1"), Duration.ofDays(1));
        Result result = connect(
                TlsContexts.create(master(), authority.certificate()),
                TlsContexts.create(expired, authority.certificate()));
        assertTrue(result.serverSawClient().isEmpty());
    }

    @Test
    @DisplayName("Der Client weist einen Server ab, dessen Zertifikat nicht zum Host passt")
    void client_rejectsServerWithWrongHost() throws Exception {
        IssuedCertificate wrongHost = authority.issueServer(
                new NodeName("master"), List.of("other.example.org"), List.of(), Duration.ofDays(1));
        Result result = connect(
                TlsContexts.create(wrongHost, authority.certificate()),
                TlsContexts.create(node("node-1"), authority.certificate()));
        assertTrue(result.failure().isPresent());
        assertTrue(result.serverSawClient().isEmpty());
    }

    @Test
    @DisplayName("Ein Client-Zertifikat darf nicht als Server-Zertifikat dienen")
    void client_certificateCannotActAsServer() throws Exception {
        IssuedCertificate clientOnly = node("node-2");
        Result result = connect(
                TlsContexts.create(clientOnly, authority.certificate()),
                TlsContexts.create(node("node-1"), authority.certificate()));
        assertTrue(result.failure().isPresent());
    }

    @Test
    @DisplayName("serverParameters verlangt TLS 1.3 und ein Client-Zertifikat")
    void serverParameters_requireTls13AndClientAuth() {
        SSLParameters parameters = TlsContexts.serverParameters();
        assertArrayEquals(new String[] {"TLSv1.3"}, parameters.getProtocols());
        assertTrue(parameters.getNeedClientAuth());
    }

    @Test
    @DisplayName("clientParameters verlangt TLS 1.3 und die Hostnamenprüfung")
    void clientParameters_requireTls13AndHostnameVerification() {
        SSLParameters parameters = TlsContexts.clientParameters();
        assertArrayEquals(new String[] {"TLSv1.3"}, parameters.getProtocols());
        assertEquals("HTTPS", parameters.getEndpointIdentificationAlgorithm());
    }

    @Test
    @DisplayName("Die Parameter-Objekte sind unabhängig voneinander veränderbar")
    void parameters_areIndependentCopies() {
        SSLParameters first = TlsContexts.serverParameters();
        first.setProtocols(new String[] {"TLSv1.2"});
        assertArrayEquals(
                new String[] {"TLSv1.3"}, TlsContexts.serverParameters().getProtocols());
    }

    @Test
    @DisplayName("create lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void create_rejectsNull() {
        assertThrows(NullPointerException.class, () -> TlsContexts.create(null, authority.certificate()));
        assertThrows(NullPointerException.class, () -> TlsContexts.create(node("node-1"), null));
    }

    @Test
    @DisplayName("Die Uhr der Zertifikatsausstellung ist unabhängig von der Systemzeit")
    void clock_isInjected() {
        Clock fixed = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);
        CertificateAuthority future = CertificateAuthority.create("Future CA", Duration.ofDays(10), fixed);
        assertEquals(
                Instant.parse("2030-01-11T00:00:00Z"),
                future.certificate().getNotAfter().toInstant());
    }
}
