package at.kilncloud.master.control;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.setup.pki.CertificateNames;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import org.jspecify.annotations.Nullable;

/**
 * Establishes who is calling, from the client certificate that the mutual-TLS handshake verified.
 *
 * <p>The interceptor reads the certificate of the TLS session, takes the node name from its common
 * name and makes it available to the service through {@link #CURRENT}. A call without a verified
 * certificate or without a valid node name in it is refused with {@code UNAUTHENTICATED} before it
 * reaches any service code. The name in the request message is never trusted on its own.
 */
public final class PeerIdentity implements ServerInterceptor {
    /** The authenticated node of the current call; only set inside an intercepted call. */
    public static final Context.Key<NodeName> CURRENT = Context.key("kiln-peer-node");

    /** Creates the interceptor. */
    public PeerIdentity() {}

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Optional<NodeName> node = nodeOf(call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION));
        if (node.isEmpty()) {
            call.close(Status.UNAUTHENTICATED.withDescription("peer.unauthenticated"), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        return Contexts.interceptCall(Context.current().withValue(CURRENT, node.get()), call, headers, next);
    }

    /**
     * Extracts the node name from a TLS session.
     *
     * @param session the TLS session of the connection, or {@code null} for an unencrypted connection
     * @return the node name, or empty if there is no verified peer certificate with a valid node name
     */
    static Optional<NodeName> nodeOf(@Nullable SSLSession session) {
        if (session == null) {
            return Optional.empty();
        }
        try {
            Certificate[] chain = session.getPeerCertificates();
            if (chain.length == 0 || !(chain[0] instanceof X509Certificate leaf)) {
                return Optional.empty();
            }
            return CertificateNames.nodeNameOf(leaf);
        } catch (SSLPeerUnverifiedException unverified) {
            return Optional.empty();
        }
    }
}
