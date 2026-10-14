package at.kilncloud.master.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.setup.pki.CertificateAuthority;
import io.grpc.Attributes;
import io.grpc.Context;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.Status;
import java.lang.reflect.Proxy;
import java.security.cert.Certificate;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link PeerIdentity}: who the caller is is decided only by the verified certificate. */
final class PeerIdentityTest {
    private final CertificateAuthority authority =
            CertificateAuthority.create("KilnCloud CA", Duration.ofDays(30), Clock.systemUTC());

    private static SSLSession session(Certificate[] chain, boolean unverified) {
        return (SSLSession) Proxy.newProxyInstance(
                PeerIdentityTest.class.getClassLoader(), new Class<?>[] {SSLSession.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getPeerCertificates")) {
                        if (unverified) {
                            throw new SSLPeerUnverifiedException("no certificate");
                        }
                        return chain;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    /** A call that records how it was closed. */
    private static final class FakeCall extends ServerCall<byte[], byte[]> {
        private final Attributes attributes;
        final List<Status> closed = new ArrayList<>();

        FakeCall(Attributes attributes) {
            this.attributes = attributes;
        }

        @Override
        public void request(int numMessages) {}

        @Override
        public void sendHeaders(Metadata headers) {}

        @Override
        public void sendMessage(byte[] message) {}

        @Override
        public void close(Status status, Metadata trailers) {
            closed.add(status);
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public MethodDescriptor<byte[], byte[]> getMethodDescriptor() {
            throw new UnsupportedOperationException();
        }
    }

    private static Attributes attributes(SSLSession session) {
        return Attributes.newBuilder()
                .set(Grpc.TRANSPORT_ATTR_SSL_SESSION, session)
                .build();
    }

    @Test
    @DisplayName("nodeOf liest den Node-Namen aus dem Client-Zertifikat der Sitzung")
    void nodeOf_readsNameFromClientCertificate() {
        Certificate leaf = authority
                .issueClient(new NodeName("node-1"), Duration.ofDays(1))
                .certificate();
        assertEquals(
                Optional.of(new NodeName("node-1")), PeerIdentity.nodeOf(session(new Certificate[] {leaf}, false)));
    }

    @Test
    @DisplayName("nodeOf liefert nichts ohne TLS-Sitzung")
    void nodeOf_emptyWithoutSession() {
        assertTrue(PeerIdentity.nodeOf(null).isEmpty());
    }

    @Test
    @DisplayName("nodeOf liefert nichts, wenn der Peer kein Zertifikat vorgelegt hat")
    void nodeOf_emptyWhenPeerUnverified() {
        assertTrue(PeerIdentity.nodeOf(session(new Certificate[0], true)).isEmpty());
    }

    @Test
    @DisplayName("nodeOf liefert nichts bei einer leeren Zertifikatskette")
    void nodeOf_emptyForEmptyChain() {
        assertTrue(PeerIdentity.nodeOf(session(new Certificate[0], false)).isEmpty());
    }

    @Test
    @DisplayName("nodeOf liefert nichts bei einem Zertifikat ohne gültigen Node-Namen")
    void nodeOf_emptyForInvalidCommonName() {
        Certificate ca = authority.certificate();
        assertTrue(PeerIdentity.nodeOf(session(new Certificate[] {ca}, false)).isEmpty());
    }

    @Test
    @DisplayName("nodeOf liefert nichts bei einem Zertifikat, das kein X.509 ist")
    void nodeOf_emptyForForeignCertificateType() {
        Certificate foreign = new Certificate("OTHER") {
            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public void verify(java.security.PublicKey key) {}

            @Override
            public void verify(java.security.PublicKey key, String sigProvider) {}

            @Override
            public String toString() {
                return "foreign";
            }

            @Override
            public java.security.PublicKey getPublicKey() {
                throw new UnsupportedOperationException();
            }
        };
        assertTrue(
                PeerIdentity.nodeOf(session(new Certificate[] {foreign}, false)).isEmpty());
    }

    @Test
    @DisplayName("interceptCall setzt den Node im Kontext und reicht den Aufruf weiter")
    void interceptCall_setsNodeInContext() {
        Certificate leaf = authority
                .issueClient(new NodeName("node-1"), Duration.ofDays(1))
                .certificate();
        FakeCall call = new FakeCall(attributes(session(new Certificate[] {leaf}, false)));
        AtomicReference<NodeName> seen = new AtomicReference<>();
        new PeerIdentity().interceptCall(call, new Metadata(), (c, h) -> {
            seen.set(PeerIdentity.CURRENT.get());
            return new ServerCall.Listener<>() {};
        });
        assertEquals(new NodeName("node-1"), seen.get());
        assertTrue(call.closed.isEmpty());
        assertNull(PeerIdentity.CURRENT.get(Context.ROOT));
    }

    @Test
    @DisplayName("interceptCall weist einen Aufruf ohne verifiziertes Zertifikat mit UNAUTHENTICATED ab")
    void interceptCall_rejectsUnauthenticatedCall() {
        FakeCall call = new FakeCall(Attributes.EMPTY);
        boolean[] reached = {false};
        new PeerIdentity().interceptCall(call, new Metadata(), (c, h) -> {
            reached[0] = true;
            return new ServerCall.Listener<>() {};
        });
        assertEquals(1, call.closed.size());
        assertEquals(Status.Code.UNAUTHENTICATED, call.closed.get(0).getCode());
        assertTrue(!reached[0], "service code must not run");
    }
}
