package at.kilncloud.node.internal;

/**
 * Starts a KilnCloud node daemon.
 *
 * <p>The M0 entry point intentionally performs no external I/O. Process supervision and master
 * communication are introduced in later milestones behind dedicated services.
 */
public final class KilnNode {
    /** Prevents construction of the process entry-point class. */
    private KilnNode() {}

    /**
     * Starts the node daemon.
     *
     * <p>This foundation entry point returns immediately and does not create processes, threads, or
     * network connections.
     *
     * @param arguments command-line arguments; ignored during M0
     */
    public static void main(String[] arguments) {}
}
