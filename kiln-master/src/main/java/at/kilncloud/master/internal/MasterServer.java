package at.kilncloud.master.internal;

import at.kilncloud.master.audit.AuditLog;
import at.kilncloud.master.config.MasterConfig;
import at.kilncloud.master.control.NodeControlService;
import at.kilncloud.master.control.PeerIdentity;
import at.kilncloud.master.registry.NodeRegistry;
import at.kilncloud.setup.pki.IssuedCertificate;
import at.kilncloud.setup.pki.PkiFiles;
import at.kilncloud.setup.token.JoinTokens;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The running master: a gRPC server that only speaks mutual TLS 1.3.
 *
 * <p>Peers without a certificate from the KilnCloud authority cannot even complete the handshake.
 * Messages are limited to 64 KiB, the TLS handshake to ten seconds, and requests run on virtual
 * threads. Close the server to stop it: it stops accepting calls, waits briefly for running ones and
 * then forces termination.
 */
public final class MasterServer implements AutoCloseable {
    private static final int MAX_MESSAGE_BYTES = 64 * 1024;
    private static final Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SHUTDOWN_GRACE = Duration.ofSeconds(5);

    private final Server server;
    private final AuditLog audit;
    private final ExecutorService executor;
    private final NodeRegistry registry;
    private final JoinTokens joinTokens;

    private MasterServer(
            Server server, AuditLog audit, ExecutorService executor, NodeRegistry registry, JoinTokens joinTokens) {
        this.server = server;
        this.audit = audit;
        this.executor = executor;
        this.registry = registry;
        this.joinTokens = joinTokens;
    }

    /**
     * Loads the identity and audit log named in the configuration and starts the server.
     *
     * @param config the validated configuration
     * @param baseDirectory the directory of the configuration file, for relative file names
     * @param clock the time source
     * @return the started server
     * @throws NullPointerException if an argument is {@code null}
     * @throws IOException if a file cannot be read, the identity is inconsistent, the audit log cannot
     *     be opened or the port cannot be bound
     */
    public static MasterServer start(MasterConfig config, Path baseDirectory, Clock clock) throws IOException {
        Objects.requireNonNull(config, "master.config.null");
        Objects.requireNonNull(baseDirectory, "master.base.null");
        Objects.requireNonNull(clock, "master.clock.null");
        IssuedCertificate identity = PkiFiles.readIdentity(
                MasterConfig.resolve(baseDirectory, config.serverCertificate()),
                MasterConfig.resolve(baseDirectory, config.serverKey()),
                MasterConfig.resolve(baseDirectory, config.authorityCertificate()));
        AuditLog audit = AuditLog.open(
                MasterConfig.resolve(baseDirectory, config.auditLog()), !config.allowUnrestrictedFilePermissions());
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            NodeRegistry registry = new NodeRegistry(clock, Duration.ofSeconds(config.heartbeatTimeoutSeconds()));
            JoinTokens joinTokens = new JoinTokens(clock);
            NodeControlService service = new NodeControlService(joinTokens, registry, audit, clock);
            SslContext tls = GrpcSslContexts.configure(
                            SslContextBuilder.forServer(identity.privateKey(), identity.certificate())
                                    .trustManager(identity.authority())
                                    .clientAuth(ClientAuth.REQUIRE)
                                    .protocols("TLSv1.3"))
                    .build();
            Server server = NettyServerBuilder.forAddress(new InetSocketAddress(config.listenHost(), config.port()))
                    .sslContext(tls)
                    .executor(executor)
                    .maxInboundMessageSize(MAX_MESSAGE_BYTES)
                    .handshakeTimeout(HANDSHAKE_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .addService(ServerInterceptors.intercept(service, new PeerIdentity()))
                    .build()
                    .start();
            return new MasterServer(server, audit, executor, registry, joinTokens);
        } catch (IOException | RuntimeException failure) {
            executor.shutdownNow();
            audit.close();
            throw failure;
        }
    }

    /**
     * Returns the port the server listens on, useful when the configuration asked for an ephemeral port.
     *
     * @return the bound TCP port
     */
    public int port() {
        return server.getPort();
    }

    /**
     * Returns the node registry.
     *
     * @return the registry shared with the service
     */
    public NodeRegistry registry() {
        return registry;
    }

    /**
     * Returns the join tokens that nodes may redeem.
     *
     * @return the token registry shared with the service
     */
    public JoinTokens joinTokens() {
        return joinTokens;
    }

    /**
     * Blocks until the server has terminated.
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }

    /**
     * Stops the server: no new calls, a short grace period for running calls, then forced termination;
     * finally the audit log is closed.
     *
     * @throws IOException if the audit log cannot be closed
     */
    @Override
    public void close() throws IOException {
        server.shutdown();
        try {
            if (!server.awaitTermination(SHUTDOWN_GRACE.toSeconds(), TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            server.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
            audit.close();
        }
    }
}
