package at.kilncloud.master.internal;

import at.kilncloud.common.config.SecureConfig;
import at.kilncloud.common.validation.NodeName;
import at.kilncloud.master.config.MasterConfig;
import at.kilncloud.setup.pki.CertificateAuthority;
import at.kilncloud.setup.pki.IssuedCertificate;
import at.kilncloud.setup.pki.PkiFiles;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * A temporary master installation: authority, master identity and configuration files on disk, plus
 * helpers to create node identities.
 */
final class MasterFixture {
    final Path dir;
    final CertificateAuthority authority;
    final boolean posix;

    MasterFixture(Path dir) throws IOException {
        this.dir = dir;
        this.posix = dir.getFileSystem().supportedFileAttributeViews().contains("posix");
        this.authority = CertificateAuthority.create("KilnCloud CA", Duration.ofDays(30), Clock.systemUTC());
        IssuedCertificate master = authority.issueServer(
                new NodeName("master"),
                List.of("localhost"),
                List.of(InetAddress.getByAddress(new byte[] {127, 0, 0, 1})),
                Duration.ofDays(1));
        PkiFiles.writeCertificate(authority.certificate(), dir.resolve("ca.crt"));
        PkiFiles.writeCertificate(master.certificate(), dir.resolve("master.crt"));
        PkiFiles.writePrivateKey(master.privateKey(), dir.resolve("master.key"), posix);
    }

    /** Finds a TCP port that is free right now. */
    static int freePort() throws IOException {
        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            channel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 1);
            return ((InetSocketAddress) channel.getLocalAddress()).getPort();
        }
    }

    MasterConfig config(int port, boolean allowUnrestricted) {
        return new MasterConfig(
                "127.0.0.1", port, "master.crt", "master.key", "ca.crt", "audit.log", 30, allowUnrestricted || !posix);
    }

    /** Writes the configuration file and returns its path. */
    Path writeConfig(int port) throws IOException {
        Path file = dir.resolve("master.json");
        SecureConfig.write(file, config(port, false), false);
        return file;
    }

    IssuedCertificate node(String name) {
        return authority.issueClient(new NodeName(name), Duration.ofDays(1));
    }
}
