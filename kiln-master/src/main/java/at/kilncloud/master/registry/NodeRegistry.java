package at.kilncloud.master.registry;

import at.kilncloud.common.validation.NodeName;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.Nullable;

/**
 * Tracks which nodes have joined the cluster and which of them are alive.
 *
 * <p>A node is <em>enrolled</em> once it has redeemed a join token; enrolment survives reconnects
 * until it is {@linkplain #revoke(NodeName) revoked}. A node is <em>connected</em> after it registers
 * and <em>online</em> while heartbeats keep arriving within the offline timeout. Heartbeat sequence
 * numbers must strictly increase per connection, which rejects replayed messages.
 *
 * <p>State is held in memory only; persistence belongs to the storage module (M4). All methods are
 * thread-safe, and a node's state changes atomically.
 */
public final class NodeRegistry {
    private final Clock clock;
    private final Duration offlineAfter;

    /** Every key is an enrolled node; a node without an entry has not joined. */
    private final ConcurrentMap<NodeName, State> nodes = new ConcurrentHashMap<>();

    /**
     * Creates an empty registry.
     *
     * @param clock the time source, injected so tests are deterministic
     * @param offlineAfter how long without a heartbeat before a node counts as offline; must be positive
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code offlineAfter} is not positive
     */
    public NodeRegistry(Clock clock, Duration offlineAfter) {
        this.clock = Objects.requireNonNull(clock, "registry.clock.null");
        this.offlineAfter = Objects.requireNonNull(offlineAfter, "registry.offline-after.null");
        if (offlineAfter.isZero() || offlineAfter.isNegative()) {
            throw new IllegalArgumentException("registry.offline-after.invalid");
        }
    }

    /**
     * Marks a node as enrolled, after it redeemed a join token. A node that is already enrolled keeps
     * its connection state.
     *
     * @param node the node
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public void enroll(NodeName node) {
        Objects.requireNonNull(node, "registry.node.null");
        nodes.putIfAbsent(node, State.DISCONNECTED);
    }

    /**
     * Tells whether a node has joined.
     *
     * @param node the node
     * @return {@code true} if the node is enrolled
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public boolean isEnrolled(NodeName node) {
        Objects.requireNonNull(node, "registry.node.null");
        return nodes.containsKey(node);
    }

    /**
     * Removes a node's enrolment and connection, for example when its certificate is compromised.
     *
     * @param node the node
     * @return {@code true} if the node was known
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public boolean revoke(NodeName node) {
        Objects.requireNonNull(node, "registry.node.null");
        return nodes.remove(node) != null;
    }

    /**
     * Records that an enrolled node opened a new connection. The heartbeat sequence starts again.
     *
     * @param node the node
     * @return {@code true} if the node is enrolled and now connected; {@code false} if it is unknown
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public boolean connect(NodeName node) {
        Objects.requireNonNull(node, "registry.node.null");
        State updated = nodes.computeIfPresent(node, (key, state) -> state.connected(clock.instant()));
        return updated != null;
    }

    /**
     * Records a heartbeat.
     *
     * @param node the node
     * @param sequence the heartbeat sequence number; must be larger than the previous one of this
     *     connection
     * @param memoryUsedMib the RAM reserved on the node, in MiB; must not be negative
     * @return what happened to the heartbeat
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public HeartbeatResult heartbeat(NodeName node, long sequence, long memoryUsedMib) {
        Objects.requireNonNull(node, "registry.node.null");
        HeartbeatResult[] result = {HeartbeatResult.NOT_REGISTERED};
        nodes.computeIfPresent(node, (key, state) -> {
            if (state.lastSeen == null) {
                return state;
            }
            if (sequence <= state.lastSequence) {
                result[0] = HeartbeatResult.REPLAYED;
                return state;
            }
            if (memoryUsedMib < 0) {
                result[0] = HeartbeatResult.INVALID;
                return state;
            }
            result[0] = HeartbeatResult.ACCEPTED;
            return state.heartbeat(sequence, memoryUsedMib, clock.instant());
        });
        return result[0];
    }

    /**
     * Tells whether a node is online: connected and heard from within the offline timeout.
     *
     * @param node the node
     * @return {@code true} if the node is online
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public boolean isOnline(NodeName node) {
        Objects.requireNonNull(node, "registry.node.null");
        State state = nodes.get(node);
        return state != null && online(state);
    }

    /**
     * Lists all enrolled nodes, sorted by name.
     *
     * @return an immutable snapshot of every node
     */
    public List<NodeSnapshot> snapshot() {
        return nodes.entrySet().stream()
                .map(entry -> new NodeSnapshot(
                        entry.getKey(),
                        online(entry.getValue()),
                        entry.getValue().memoryUsedMib,
                        Optional.ofNullable(entry.getValue().lastSeen)))
                .sorted(Comparator.comparing(snapshot -> snapshot.node().value()))
                .toList();
    }

    private boolean online(State state) {
        Instant lastSeen = state.lastSeen;
        return lastSeen != null && clock.instant().isBefore(lastSeen.plus(offlineAfter));
    }

    /**
     * Immutable state of one enrolled node; replaced as a whole so updates are atomic. A node is
     * connected exactly when {@code lastSeen} is set.
     */
    private static final class State {
        static final State DISCONNECTED = new State(0, 0, null);

        final long lastSequence;
        final long memoryUsedMib;
        final @Nullable Instant lastSeen;

        private State(long lastSequence, long memoryUsedMib, @Nullable Instant lastSeen) {
            this.lastSequence = lastSequence;
            this.memoryUsedMib = memoryUsedMib;
            this.lastSeen = lastSeen;
        }

        State connected(Instant now) {
            return new State(0, memoryUsedMib, now);
        }

        State heartbeat(long sequence, long memory, Instant now) {
            return new State(sequence, memory, now);
        }
    }
}
