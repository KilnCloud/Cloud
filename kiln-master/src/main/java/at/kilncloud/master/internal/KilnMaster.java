package at.kilncloud.master.internal;

/**
 * Starts the central KilnCloud master process.
 *
 * <p>The M0 entry point intentionally performs no external I/O. Lifecycle, networking, and
 * configuration are introduced in later milestones behind dedicated services.
 */
public final class KilnMaster {
    /** Prevents construction of the process entry-point class. */
    private KilnMaster() {}

    /**
     * Starts the master process.
     *
     * <p>This foundation entry point returns immediately and does not create threads or access the
     * network.
     *
     * @param arguments command-line arguments; ignored during M0
     */
    public static void main(String[] arguments) {}
}
