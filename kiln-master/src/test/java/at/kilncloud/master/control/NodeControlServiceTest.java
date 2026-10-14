package at.kilncloud.master.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.master.TestClock;
import at.kilncloud.master.audit.AuditLog;
import at.kilncloud.master.registry.NodeRegistry;
import at.kilncloud.protocol.ProtocolVersions;
import at.kilncloud.protocol.v1.HeartbeatRequest;
import at.kilncloud.protocol.v1.HeartbeatResponse;
import at.kilncloud.protocol.v1.ProtocolVersion;
import at.kilncloud.protocol.v1.RegisterRequest;
import at.kilncloud.protocol.v1.RegisterResponse;
import at.kilncloud.protocol.v1.RegisterStatus;
import at.kilncloud.setup.token.JoinTokens;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.protobuf.ByteString;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the decisions of {@link NodeControlService}, with the authenticated peer injected. */
final class NodeControlServiceTest {
    private static final NodeName NODE = new NodeName("node-1");

    /** Collects what a service call sends back. */
    private static final class Recorder<T> implements StreamObserver<T> {
        final List<T> values = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();
        boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            errors.add(throwable);
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        Status.Code errorCode() {
            return ((StatusRuntimeException) errors.get(0)).getStatus().getCode();
        }
    }

    private final TestClock clock = new TestClock(Instant.parse("2026-10-14T12:00:00Z"));
    private final JoinTokens tokens = new JoinTokens(clock);
    private final NodeRegistry registry = new NodeRegistry(clock, Duration.ofSeconds(30));
    private FileSystem unix;
    private Path auditFile;
    private AuditLog audit;
    private NodeControlService service;

    @BeforeEach
    void setUp() throws IOException {
        unix = Jimfs.newFileSystem(Configuration.unix().toBuilder()
                .setAttributeViews("basic", "posix")
                .build());
        auditFile = Files.createDirectories(unix.getPath("/log")).resolve("audit.log");
        audit = AuditLog.open(auditFile, true);
        service = new NodeControlService(tokens, registry, audit, clock);
    }

    @AfterEach
    void tearDown() throws IOException {
        audit.close();
        unix.close();
    }

    private static RegisterRequest.Builder request(String name, byte[] token) {
        return RegisterRequest.newBuilder()
                .setClientVersion(ProtocolVersions.current())
                .setNodeName(name)
                .setJoinToken(ByteString.copyFrom(token));
    }

    private Recorder<RegisterResponse> register(NodeName peer, RegisterRequest request) {
        Recorder<RegisterResponse> out = new Recorder<>();
        Context.current().withValue(PeerIdentity.CURRENT, peer).run(() -> service.register(request, out));
        return out;
    }

    private Recorder<HeartbeatResponse> heartbeat(NodeName peer, long sequence, long memory) {
        Recorder<HeartbeatResponse> out = new Recorder<>();
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setSequence(sequence)
                .setMemoryUsedMib(memory)
                .build();
        Context.current().withValue(PeerIdentity.CURRENT, peer).run(() -> service.heartbeat(request, out));
        return out;
    }

    private RegisterStatus statusOf(Recorder<RegisterResponse> out) {
        assertEquals(1, out.values.size());
        assertTrue(out.completed);
        assertEquals(ProtocolVersions.current(), out.values.get(0).getServerVersion());
        return out.values.get(0).getStatus();
    }

    private List<String> auditLines() throws IOException {
        return Files.readAllLines(auditFile);
    }

    @Test
    @DisplayName("register nimmt einen Node mit gültigem Join-Token auf, schreibt Audit und verbindet ihn")
    void register_acceptsValidToken() throws IOException {
        byte[] token = tokens.issue(NODE, Duration.ofMinutes(5));
        assertEquals(
                RegisterStatus.REGISTER_STATUS_OK,
                statusOf(register(NODE, request("node-1", token).build())));
        assertTrue(registry.isEnrolled(NODE));
        assertTrue(registry.isOnline(NODE));
        assertTrue(auditLines().get(0).contains("\"outcome\":\"accepted\""));
    }

    @Test
    @DisplayName("register lässt dasselbe Token kein zweites Mal zu")
    void register_rejectsReusedToken() {
        byte[] token = tokens.issue(NODE, Duration.ofMinutes(5));
        register(NODE, request("node-1", token).build());
        registry.revoke(NODE);
        assertEquals(
                RegisterStatus.REGISTER_STATUS_TOKEN_REJECTED,
                statusOf(register(NODE, request("node-1", token).build())));
        assertFalse(registry.isEnrolled(NODE));
    }

    @Test
    @DisplayName("register erlaubt einem bereits aufgenommenen Node die Wiederverbindung ohne Token")
    void register_reconnectWithoutToken() {
        register(
                NODE,
                request("node-1", tokens.issue(NODE, Duration.ofMinutes(5))).build());
        assertEquals(
                RegisterStatus.REGISTER_STATUS_OK,
                statusOf(register(NODE, request("node-1", new byte[0]).build())));
    }

    @Test
    @DisplayName("register verlangt für einen neuen Node ein Token")
    void register_requiresTokenForNewNode() {
        assertEquals(
                RegisterStatus.REGISTER_STATUS_TOKEN_REJECTED,
                statusOf(register(NODE, request("node-1", new byte[0]).build())));
        assertFalse(registry.isEnrolled(NODE));
    }

    @Test
    @DisplayName("register lehnt ein falsches und ein zu langes Token ab")
    void register_rejectsWrongAndOversizedToken() {
        assertEquals(
                RegisterStatus.REGISTER_STATUS_TOKEN_REJECTED,
                statusOf(register(NODE, request("node-1", new byte[32]).build())));
        assertEquals(
                RegisterStatus.REGISTER_STATUS_TOKEN_REJECTED,
                statusOf(register(
                        NODE,
                        request("node-1", new byte[NodeControlService.MAX_TOKEN_BYTES + 1])
                                .build())));
    }

    @Test
    @DisplayName("register lehnt einen Namen ab, der nicht zum Zertifikat passt")
    void register_rejectsNameThatDiffersFromCertificate() {
        byte[] token = tokens.issue(new NodeName("node-2"), Duration.ofMinutes(5));
        assertEquals(
                RegisterStatus.REGISTER_STATUS_NAME_REJECTED,
                statusOf(register(NODE, request("node-2", token).build())));
        assertFalse(registry.isEnrolled(new NodeName("node-2")));
        assertTrue(tokens.redeem(new NodeName("node-2"), token), "the token of another node must stay unused");
    }

    @Test
    @DisplayName("register lehnt einen ungültigen Namen in der Anfrage ab")
    void register_rejectsInvalidRequestedName() {
        byte[] token = tokens.issue(NODE, Duration.ofMinutes(5));
        assertEquals(
                RegisterStatus.REGISTER_STATUS_NAME_REJECTED,
                statusOf(register(NODE, request("../etc", token).build())));
    }

    @Test
    @DisplayName("register lehnt eine fehlende oder inkompatible Protokollversion ab")
    void register_rejectsIncompatibleVersion() {
        byte[] token = tokens.issue(NODE, Duration.ofMinutes(5));
        RegisterRequest missing = RegisterRequest.newBuilder()
                .setNodeName("node-1")
                .setJoinToken(ByteString.copyFrom(token))
                .build();
        RegisterRequest future = request("node-1", token)
                .setClientVersion(ProtocolVersion.newBuilder().setMajor(2).build())
                .build();
        assertEquals(RegisterStatus.REGISTER_STATUS_VERSION_INCOMPATIBLE, statusOf(register(NODE, missing)));
        assertEquals(RegisterStatus.REGISTER_STATUS_VERSION_INCOMPATIBLE, statusOf(register(NODE, future)));
        assertTrue(tokens.redeem(NODE, token), "a refused version must not use up the token");
    }

    @Test
    @DisplayName("register protokolliert eine Ablehnung im Audit-Log, ohne das Token zu nennen")
    void register_auditsDenialWithoutToken() throws IOException {
        register(NODE, request("node-1", new byte[32]).build());
        String line = auditLines().get(0);
        assertTrue(line.contains("denied:REGISTER_STATUS_TOKEN_REJECTED"));
        assertFalse(line.contains("AAAA"));
    }

    @Test
    @DisplayName("register scheitert geschlossen, wenn das Audit-Log nicht schreibbar ist")
    void register_failsClosedWhenAuditUnavailable() throws IOException {
        byte[] token = tokens.issue(NODE, Duration.ofMinutes(5));
        audit.close();
        Recorder<RegisterResponse> out = register(NODE, request("node-1", token).build());
        assertEquals(Status.Code.INTERNAL, out.errorCode());
        assertFalse(registry.isEnrolled(NODE), "an action that cannot be audited must not happen");
    }

    @Test
    @DisplayName("register verlangt einen authentifizierten Peer")
    void register_requiresAuthenticatedPeer() {
        Recorder<RegisterResponse> out = new Recorder<>();
        service.register(request("node-1", new byte[0]).build(), out);
        assertEquals(Status.Code.UNAUTHENTICATED, out.errorCode());
    }

    @Test
    @DisplayName("heartbeat wird für einen registrierten Node bestätigt")
    void heartbeat_acknowledgesRegisteredNode() {
        register(
                NODE,
                request("node-1", tokens.issue(NODE, Duration.ofMinutes(5))).build());
        Recorder<HeartbeatResponse> out = heartbeat(NODE, 7, 512);
        assertEquals(7, out.values.get(0).getSequence());
        assertTrue(out.completed);
    }

    @Test
    @DisplayName("heartbeat wird ohne Registrierung verweigert")
    void heartbeat_deniedWithoutRegistration() {
        assertEquals(Status.Code.PERMISSION_DENIED, heartbeat(NODE, 1, 0).errorCode());
    }

    @Test
    @DisplayName("heartbeat weist eine wiederholte Sequenz und negative Werte ab")
    void heartbeat_rejectsReplayAndInvalidValues() {
        register(
                NODE,
                request("node-1", tokens.issue(NODE, Duration.ofMinutes(5))).build());
        heartbeat(NODE, 1, 0);
        assertEquals(Status.Code.FAILED_PRECONDITION, heartbeat(NODE, 1, 0).errorCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, heartbeat(NODE, 2, -5).errorCode());
    }

    @Test
    @DisplayName("heartbeat verlangt einen authentifizierten Peer")
    void heartbeat_requiresAuthenticatedPeer() {
        Recorder<HeartbeatResponse> out = new Recorder<>();
        service.heartbeat(HeartbeatRequest.getDefaultInstance(), out);
        assertEquals(Status.Code.UNAUTHENTICATED, out.errorCode());
    }

    @Test
    @DisplayName("Der Konstruktor lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void constructor_rejectsNull() {
        assertThrows(NullPointerException.class, () -> new NodeControlService(null, registry, audit, clock));
        assertThrows(NullPointerException.class, () -> new NodeControlService(tokens, null, audit, clock));
        assertThrows(NullPointerException.class, () -> new NodeControlService(tokens, registry, null, clock));
        assertThrows(NullPointerException.class, () -> new NodeControlService(tokens, registry, audit, null));
    }
}
