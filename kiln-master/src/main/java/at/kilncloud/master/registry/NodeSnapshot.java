package at.kilncloud.master.registry;

import at.kilncloud.common.validation.NodeName;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of one enrolled node at a point in time.
 *
 * @param node the node
 * @param online whether the node has an active connection and sent a heartbeat recently
 * @param memoryUsedMib RAM reserved on the node as last reported, in MiB; {@code 0} before any report
 * @param lastSeen when the node was last heard from, if ever
 */
public record NodeSnapshot(NodeName node, boolean online, long memoryUsedMib, Optional<Instant> lastSeen) {
    /**
     * Creates a snapshot.
     *
     * @throws NullPointerException if {@code node} or {@code lastSeen} is {@code null}
     */
    public NodeSnapshot {
        Objects.requireNonNull(node, "registry.node.null");
        Objects.requireNonNull(lastSeen, "registry.last-seen.null");
    }
}
