package at.kilncloud.master.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.protocol.ProtocolVersions;
import at.kilncloud.protocol.v1.HeartbeatRequest;
import at.kilncloud.protocol.v1.NodeControlGrpc;
import at.kilncloud.protocol.v1.RegisterRequest;
import at.kilncloud.protocol.v1.RegisterResponse;
import at.kilncloud.protocol.v1.RegisterStatus;
import at.kilncloud.setup.pki.CertificateAuthority;
import at.kilncloud.setup.pki.IssuedCertificate;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end tests over real mutual TLS 1.3 and gRPC on the loopback interface: the whole path from
 * the TLS handshake to the audit log.
 */
final class MasterServerTest {
    @TempDir
    Path dir;

    private MasterFixture fixture;
    private MasterServer server;

    @BeforeEach
    void setUp() throws IOException {
        fixture = new MasterFixture(dir);
        server = MasterServer.start(fixture.config(MasterFixture.freePort(), false), dir, Clock.systemUTC());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    private ManagedChannel channel(IssuedCertificate identity, CertificateAuthority trusted) throws SSLException {
        return build(
                SslContextBuilder.forClient()
                        .protocols("TLSv1.3")
                        .keyManager(identity.privateKey(), identity.certificate()),
                trusted);
    }

    /** A client that trusts the master but presents no certificate of its own. */
    private ManagedChannel anonymousChannel(CertificateAuthority trusted) throws SSLException {
        return build(SslContextBuilder.forClient().protocols("TLSv1.3"), trusted);
    }

    private ManagedChannel build(SslContextBuilder builder, CertificateAuthority trusted) throws SSLException {
        SslContext tls = GrpcSslContexts.configure(builder.trustManager(trusted.certificate()))
                .build();
        return NettyChannelBuilder.forAddress("localhost", server.port())
                .sslContext(tls)
                .build();
    }

    private static NodeControlGrpc.NodeControlBlockingStub stub(ManagedChannel channel) {
        return NodeControlGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS);
    }

    private static RegisterRequest register(String name, byte[] token) {
        return RegisterRequest.newBuilder()
                .setClientVersion(ProtocolVersions.current())
                .setNodeName(name)
                .setJoinToken(ByteString.copyFrom(token))
                .build();
    }

    private static void close(ManagedChannel channel) throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Ein Node registriert sich per mTLS und Join-Token und sendet Heartbeats")
    void node_registersAndSendsHeartbeats() throws Exception {
        NodeName name = new NodeName("node-1");
        byte[] token = server.joinTokens().issue(name, Duration.ofMinutes(5));
        ManagedChannel channel = channel(fixture.node("node-1"), fixture.authority);
        try {
            RegisterResponse response = stub(channel).register(register("node-1", token));
            assertEquals(RegisterStatus.REGISTER_STATUS_OK, response.getStatus());
            assertTrue(server.registry().isOnline(name));
            long echoed = stub(channel)
                    .heartbeat(HeartbeatRequest.newBuilder()
                            .setSequence(1)
                            .setMemoryUsedMib(1024)
                            .build())
                    .getSequence();
            assertEquals(1, echoed);
            assertEquals(1024, server.registry().snapshot().get(0).memoryUsedMib());
        } finally {
            close(channel);
        }
        close(channel);
        server.close();
        List<String> audit = Files.readAllLines(dir.resolve("audit.log"));
        assertEquals(1, audit.size());
        assertTrue(audit.get(0).contains("\"action\":\"node.register\"")
                && audit.get(0).contains("accepted"));
        assertFalse(audit.get(0).toLowerCase(java.util.Locale.ROOT).contains("token"));
    }

    @Test
    @DisplayName("Dasselbe Join-Token funktioniert kein zweites Mal")
    void joinToken_worksOnlyOnce() throws Exception {
        NodeName name = new NodeName("node-1");
        byte[] token = server.joinTokens().issue(name, Duration.ofMinutes(5));
        ManagedChannel channel = channel(fixture.node("node-1"), fixture.authority);
        try {
            assertEquals(
                    RegisterStatus.REGISTER_STATUS_OK,
                    stub(channel).register(register("node-1", token)).getStatus());
            assertEquals(
                    RegisterStatus.REGISTER_STATUS_TOKEN_REJECTED,
                    stub(channel).register(register("node-1", token)).getStatus());
        } finally {
            close(channel);
        }
    }

    @Test
    @DisplayName("Ein Node kann sich nicht unter dem Namen eines anderen registrieren")
    void node_cannotRegisterAsAnotherNode() throws Exception {
        byte[] token = server.joinTokens().issue(new NodeName("node-2"), Duration.ofMinutes(5));
        ManagedChannel channel = channel(fixture.node("node-1"), fixture.authority);
        try {
            assertEquals(
                    RegisterStatus.REGISTER_STATUS_NAME_REJECTED,
                    stub(channel).register(register("node-2", token)).getStatus());
            assertFalse(server.registry().isEnrolled(new NodeName("node-2")));
        } finally {
            close(channel);
        }
    }

    @Test
    @DisplayName("Ein Heartbeat vor der Registrierung wird verweigert")
    void heartbeat_beforeRegistrationDenied() throws Exception {
        ManagedChannel channel = channel(fixture.node("node-1"), fixture.authority);
        try {
            StatusRuntimeException failure = assertThrows(
                    StatusRuntimeException.class,
                    () -> stub(channel)
                            .heartbeat(
                                    HeartbeatRequest.newBuilder().setSequence(1).build()));
            assertEquals(Status.Code.PERMISSION_DENIED, failure.getStatus().getCode());
        } finally {
            close(channel);
        }
    }

    @Test
    @DisplayName("Ein Client ohne Zertifikat kann keinen Aufruf ausführen")
    void clientWithoutCertificate_isRefused() throws Exception {
        ManagedChannel channel = anonymousChannel(fixture.authority);
        try {
            assertThrows(StatusRuntimeException.class, () -> stub(channel).register(register("node-1", new byte[32])));
        } finally {
            close(channel);
        }
    }

    @Test
    @DisplayName("Ein Client mit Zertifikat einer fremden CA kann keinen Aufruf ausführen")
    void clientFromOtherAuthority_isRefused() throws Exception {
        CertificateAuthority other = CertificateAuthority.create("Evil CA", Duration.ofDays(1), Clock.systemUTC());
        ManagedChannel channel =
                channel(other.issueClient(new NodeName("node-1"), Duration.ofDays(1)), fixture.authority);
        try {
            assertThrows(StatusRuntimeException.class, () -> stub(channel).register(register("node-1", new byte[32])));
        } finally {
            close(channel);
        }
    }

    @Test
    @DisplayName("Zu große Nachrichten werden abgelehnt")
    void oversizedMessage_isRejected() throws Exception {
        ManagedChannel channel = channel(fixture.node("node-1"), fixture.authority);
        try {
            StatusRuntimeException failure = assertThrows(
                    StatusRuntimeException.class,
                    () -> stub(channel).register(register("node-1", new byte[70 * 1024])));
            assertEquals(Status.Code.RESOURCE_EXHAUSTED, failure.getStatus().getCode());
        } finally {
            close(channel);
        }
    }

    @Test
    @DisplayName("start scheitert mit IOException, wenn der Port schon belegt ist")
    void start_failsWhenPortInUse() {
        assertThrows(
                IOException.class,
                () -> MasterServer.start(fixture.config(server.port(), false), dir, Clock.systemUTC()));
    }

    @Test
    @DisplayName("start scheitert, wenn Schlüssel und Zertifikat nicht zusammenpassen")
    void start_failsForInconsistentIdentity() throws Exception {
        IssuedCertificate other = fixture.node("node-9");
        at.kilncloud.setup.pki.PkiFiles.writePrivateKey(other.privateKey(), dir.resolve("master.key"), fixture.posix);
        IOException failure = assertThrows(
                IOException.class,
                () -> MasterServer.start(fixture.config(MasterFixture.freePort(), false), dir, Clock.systemUTC()));
        assertEquals("pki.identity.key-mismatch", failure.getMessage());
    }

    @Test
    @DisplayName("start lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void start_rejectsNull() {
        assertThrows(NullPointerException.class, () -> MasterServer.start(null, dir, Clock.systemUTC()));
        assertThrows(
                NullPointerException.class,
                () -> MasterServer.start(fixture.config(1, false), null, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> MasterServer.start(fixture.config(1, false), dir, null));
    }

    @Test
    @DisplayName("close beendet den Server")
    void close_stopsServer() throws Exception {
        // The channel must be built first: port() is not available once the server has terminated.
        ManagedChannel channel = channel(fixture.node("node-1"), fixture.authority);
        server.close();
        try {
            assertThrows(StatusRuntimeException.class, () -> stub(channel).register(register("node-1", new byte[32])));
        } finally {
            close(channel);
        }
    }
}
