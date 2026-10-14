package at.kilncloud.master.config;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration of the master process, read from a JSON file with {@code SecureConfig}.
 *
 * <p>Every field is required except {@code allowUnrestrictedFilePermissions}, which is {@code false}
 * when absent: the secure setting is the default (AGENTS.md §7.1). File names are relative to the
 * directory of the configuration file unless absolute.
 *
 * @param listenHost the address to listen on, for example {@code 0.0.0.0} or a single interface; not
 *     blank
 * @param port the TCP port, 1 to 65535
 * @param serverCertificate file with the master's TLS certificate
 * @param serverKey file with the master's TLS private key
 * @param authorityCertificate file with the certificate of the KilnCloud authority that signs all
 *     identities
 * @param auditLog file of the append-only audit log
 * @param heartbeatTimeoutSeconds seconds without a heartbeat before a node counts as offline, 5 to 3600
 * @param allowUnrestrictedFilePermissions {@code true} only for development on systems without POSIX
 *     file permissions; it disables the owner-only protection of the audit log and logs a warning
 */
public record MasterConfig(
        String listenHost,
        int port,
        String serverCertificate,
        String serverKey,
        String authorityCertificate,
        String auditLog,
        int heartbeatTimeoutSeconds,
        boolean allowUnrestrictedFilePermissions) {
    /** Longest accepted host name or path text. */
    private static final int MAX_TEXT = 4096;

    /**
     * Creates a validated configuration.
     *
     * @throws NullPointerException if a text is {@code null}
     * @throws IllegalArgumentException if a text is blank, too long or not a valid path, the port or
     *     the timeout is out of range
     */
    public MasterConfig {
        requireText(listenHost, "master.config.listen-host");
        requireText(serverCertificate, "master.config.server-certificate");
        requireText(serverKey, "master.config.server-key");
        requireText(authorityCertificate, "master.config.authority-certificate");
        requireText(auditLog, "master.config.audit-log");
        requirePath(serverCertificate, "master.config.server-certificate");
        requirePath(serverKey, "master.config.server-key");
        requirePath(authorityCertificate, "master.config.authority-certificate");
        requirePath(auditLog, "master.config.audit-log");
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("master.config.port");
        }
        if (heartbeatTimeoutSeconds < 5 || heartbeatTimeoutSeconds > 3600) {
            throw new IllegalArgumentException("master.config.heartbeat-timeout");
        }
    }

    /**
     * Resolves a file name from this configuration.
     *
     * @param baseDirectory the directory of the configuration file
     * @param name one of the file names of this configuration
     * @return the absolute, normalized path; absolute names stay as they are
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Path resolve(Path baseDirectory, String name) {
        Objects.requireNonNull(baseDirectory, "master.config.base.null");
        Objects.requireNonNull(name, "master.config.name.null");
        return baseDirectory.resolve(name).toAbsolutePath().normalize();
    }

    private static void requireText(String text, String key) {
        Objects.requireNonNull(text, key);
        if (text.isBlank() || text.length() > MAX_TEXT) {
            throw new IllegalArgumentException(key);
        }
    }

    private static void requirePath(String text, String key) {
        try {
            Path parsed = Path.of(text);
            if (parsed.toString().isBlank()) {
                throw new IllegalArgumentException(key);
            }
        } catch (InvalidPathException invalid) {
            throw new IllegalArgumentException(key);
        }
    }
}
