package at.kilncloud.master.control;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.master.audit.AuditEvent;
import at.kilncloud.master.audit.AuditLog;
import at.kilncloud.master.registry.HeartbeatResult;
import at.kilncloud.master.registry.NodeRegistry;
import at.kilncloud.protocol.ProtocolVersions;
import at.kilncloud.protocol.v1.HeartbeatRequest;
import at.kilncloud.protocol.v1.HeartbeatResponse;
import at.kilncloud.protocol.v1.NodeControlGrpc;
import at.kilncloud.protocol.v1.RegisterRequest;
import at.kilncloud.protocol.v1.RegisterResponse;
import at.kilncloud.protocol.v1.RegisterStatus;
import at.kilncloud.setup.token.JoinTokens;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;

/**
 * The master side of the node control protocol.
 *
 * <p>Security rules, in order: the caller must hold a valid client certificate (checked by {@link
 * PeerIdentity} before this class is reached); the protocol version must be compatible; the node name
 * in the request must equal the name in the certificate, so a node cannot register as another;
 * a first-time node must present a valid one-time join token; a node that already joined may
 * reconnect without one. Every decision is written to the audit log <em>before</em> it takes effect,
 * and if the log cannot be written the request fails (fail closed). Requests are never logged,
 * because they carry the join token.
 */
public final class NodeControlService extends NodeControlGrpc.NodeControlImplBase {
    /** Longest accepted join token, so oversized input is rejected before hashing. */
    static final int MAX_TOKEN_BYTES = 256;

    private final JoinTokens tokens;
    private final NodeRegistry registry;
    private final AuditLog audit;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param tokens the issued join tokens
     * @param registry the node registry
     * @param audit the audit log
     * @param clock the time source for audit entries
     * @throws NullPointerException if an argument is {@code null}
     */
    public NodeControlService(JoinTokens tokens, NodeRegistry registry, AuditLog audit, Clock clock) {
        this.tokens = Objects.requireNonNull(tokens, "control.tokens.null");
        this.registry = Objects.requireNonNull(registry, "control.registry.null");
        this.audit = Objects.requireNonNull(audit, "control.audit.null");
        this.clock = Objects.requireNonNull(clock, "control.clock.null");
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        NodeName peer = PeerIdentity.CURRENT.get();
        if (peer == null) {
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("peer.unauthenticated")
                    .asRuntimeException());
            return;
        }
        RegisterStatus status = decide(peer, request);
        try {
            record(
                    peer.value(),
                    "node.register",
                    peer.value(),
                    status == RegisterStatus.REGISTER_STATUS_OK ? "accepted" : "denied:" + status.name());
        } catch (IOException auditFailure) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("audit.unavailable").asRuntimeException());
            return;
        }
        if (status == RegisterStatus.REGISTER_STATUS_OK) {
            registry.enroll(peer);
            registry.connect(peer);
        }
        responseObserver.onNext(RegisterResponse.newBuilder()
                .setServerVersion(ProtocolVersions.current())
                .setStatus(status)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void heartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        NodeName peer = PeerIdentity.CURRENT.get();
        if (peer == null) {
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("peer.unauthenticated")
                    .asRuntimeException());
            return;
        }
        // Protobuf uint64 arrives as a signed long; values above 2^63 appear negative and are invalid.
        HeartbeatResult result = registry.heartbeat(peer, request.getSequence(), request.getMemoryUsedMib());
        switch (result) {
            case ACCEPTED -> {
                responseObserver.onNext(HeartbeatResponse.newBuilder()
                        .setSequence(request.getSequence())
                        .build());
                responseObserver.onCompleted();
            }
            case NOT_REGISTERED ->
                responseObserver.onError(Status.PERMISSION_DENIED
                        .withDescription("node.not-registered")
                        .asRuntimeException());
            case REPLAYED ->
                responseObserver.onError(Status.FAILED_PRECONDITION
                        .withDescription("heartbeat.replayed")
                        .asRuntimeException());
            case INVALID ->
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("heartbeat.invalid")
                        .asRuntimeException());
        }
    }

    private RegisterStatus decide(NodeName peer, RegisterRequest request) {
        if (!request.hasClientVersion() || !ProtocolVersions.isCompatible(request.getClientVersion())) {
            return RegisterStatus.REGISTER_STATUS_VERSION_INCOMPATIBLE;
        }
        if (!nameMatches(peer, request.getNodeName())) {
            return RegisterStatus.REGISTER_STATUS_NAME_REJECTED;
        }
        byte[] token = request.getJoinToken().toByteArray();
        try {
            if (token.length > MAX_TOKEN_BYTES) {
                return RegisterStatus.REGISTER_STATUS_TOKEN_REJECTED;
            }
            if (token.length == 0) {
                return registry.isEnrolled(peer)
                        ? RegisterStatus.REGISTER_STATUS_OK
                        : RegisterStatus.REGISTER_STATUS_TOKEN_REJECTED;
            }
            return tokens.redeem(peer, token)
                    ? RegisterStatus.REGISTER_STATUS_OK
                    : RegisterStatus.REGISTER_STATUS_TOKEN_REJECTED;
        } finally {
            Arrays.fill(token, (byte) 0);
        }
    }

    private static boolean nameMatches(NodeName peer, String requested) {
        try {
            return new NodeName(requested).equals(peer);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private void record(String actor, String action, String target, String outcome) throws IOException {
        audit.append(new AuditEvent(clock.instant(), actor, action, target, outcome));
    }
}
