package at.kilncloud.master.internal;

import at.kilncloud.common.config.ConfigException;
import at.kilncloud.common.config.SecureConfig;
import at.kilncloud.master.config.MasterConfig;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/**
 * Starts the central KilnCloud master process.
 *
 * <p>Usage: {@code kiln-master <config.json>}. The configuration is read strictly (unknown fields are
 * rejected), the server starts, and the process runs until it receives a termination signal.
 */
public final class KilnMaster {
    /** Exit code for a successful run. */
    static final int EXIT_OK = 0;

    /** Exit code for a missing or invalid command line. */
    static final int EXIT_USAGE = 2;

    /** Exit code for a configuration or start-up failure. */
    static final int EXIT_FAILURE = 1;

    /** Prevents construction of the process entry-point class. */
    private KilnMaster() {}

    /**
     * Runs the master and exits with a non-zero code if it cannot start.
     *
     * @param arguments command-line arguments: the path of the configuration file
     */
    public static void main(String[] arguments) {
        int code = run(arguments, System.err, true);
        if (code != EXIT_OK) {
            System.exit(code);
        }
    }

    /**
     * Starts the master and, if {@code blocking}, waits for its termination.
     *
     * <p>Error messages are neutral keys on {@code errors}; secrets and file contents are never
     * printed. This method blocks the calling thread while the server runs.
     *
     * @param arguments command-line arguments: exactly one, the path of the configuration file
     * @param errors where to print error keys
     * @param blocking whether to wait until the server terminates; tests pass {@code false}
     * @return the process exit code: {@code 0} on success, {@code 2} for a bad command line,
     *     {@code 1} if the configuration or start-up fails
     * @throws NullPointerException if an argument is {@code null}
     */
    static int run(String[] arguments, PrintStream errors, boolean blocking) {
        Objects.requireNonNull(arguments, "master.arguments.null");
        Objects.requireNonNull(errors, "master.errors.null");
        if (arguments.length != 1) {
            errors.println("master.usage: kiln-master <config.json>");
            return EXIT_USAGE;
        }
        Path configFile = Path.of(arguments[0]).toAbsolutePath();
        Path directory = configFile.getParent();
        if (directory == null) {
            errors.println("master.config.no-directory");
            return EXIT_FAILURE;
        }
        try {
            MasterConfig config = SecureConfig.read(configFile, MasterConfig.class);
            if (config.allowUnrestrictedFilePermissions()) {
                errors.println("master.warning.unrestricted-file-permissions");
            }
            try (MasterServer server = MasterServer.start(config, directory, Clock.systemUTC())) {
                if (blocking) {
                    Thread shutdown = Thread.ofPlatform().unstarted(() -> closeQuietly(server));
                    Runtime.getRuntime().addShutdownHook(shutdown);
                    server.awaitTermination();
                }
            }
            return EXIT_OK;
        } catch (ConfigException invalid) {
            errors.println(invalid.getMessage());
            return EXIT_FAILURE;
        } catch (IOException failure) {
            errors.println("master.start.failed");
            return EXIT_FAILURE;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return EXIT_OK;
        }
    }

    private static void closeQuietly(MasterServer server) {
        try {
            server.close();
        } catch (IOException ignored) {
            // The process is ending; nothing is left to report to.
        }
    }
}
