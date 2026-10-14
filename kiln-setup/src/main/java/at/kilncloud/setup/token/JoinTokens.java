package at.kilncloud.setup.token;

import at.kilncloud.common.security.SecureTokens;
import at.kilncloud.common.validation.NodeName;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues and redeems one-time tokens with which a new node joins the cluster (AGENTS.md §7.2).
 *
 * <p>A token is 32 random bytes, valid for a short time, bound to one node name, and usable exactly
 * once. Only the SHA-256 hash is kept, so a leaked registry does not reveal usable tokens. Hashing
 * without a salt is sufficient because the tokens are random with 256 bits of entropy, not chosen by
 * people. Hashes are compared in constant time against every stored entry.
 *
 * <p>This registry lives in memory. Persisting it belongs to the storage module (M4). It is
 * thread-safe; redeeming a token succeeds for only one of several concurrent callers.
 */
public final class JoinTokens {
    /** Longest lifetime of a token. */
    public static final Duration MAX_TTL = Duration.ofHours(24);

    /** Upper bound on tokens held at once, so a flood of requests cannot exhaust memory. */
    static final int MAX_OUTSTANDING = 1000;

    private static final int TOKEN_BYTES = 32;

    private final Clock clock;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    /**
     * Creates an empty registry.
     *
     * @param clock the time source, injected so tests are deterministic
     * @throws NullPointerException if {@code clock} is {@code null}
     */
    public JoinTokens(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "token.clock.null");
    }

    /**
     * Issues a token for a node.
     *
     * <p>The returned bytes are the only copy of the token. Hand them to the operator once and
     * overwrite the array afterwards.
     *
     * @param node the node that may redeem the token
     * @param ttl how long the token stays valid; positive and at most {@link #MAX_TTL}
     * @return the new token
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code ttl} is out of range
     * @throws IllegalStateException if too many unexpired tokens are outstanding
     */
    public byte[] issue(NodeName node, Duration ttl) {
        Objects.requireNonNull(node, "token.node.null");
        Objects.requireNonNull(ttl, "token.ttl.null");
        if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAX_TTL) > 0) {
            throw new IllegalArgumentException("token.ttl.invalid");
        }
        purgeExpired();
        if (entries.size() >= MAX_OUTSTANDING) {
            throw new IllegalStateException("token.limit.reached");
        }
        byte[] token = SecureTokens.generate(TOKEN_BYTES);
        entries.put(
                UUID.randomUUID(), new Entry(node, hash(token), clock.instant().plus(ttl)));
        return token;
    }

    /**
     * Redeems a token. A valid token works exactly once and only for the node it was issued for.
     *
     * <p>A wrong node name does not use up the token, so a typo does not lock the operator out, but
     * an attacker who guesses a node name still needs the token itself.
     *
     * @param node the node that presents the token
     * @param token the token bytes; not modified
     * @return {@code true} if the token was valid, unexpired, issued for {@code node}, and not used
     *     before; {@code false} otherwise
     * @throws NullPointerException if an argument is {@code null}
     */
    public boolean redeem(NodeName node, byte[] token) {
        Objects.requireNonNull(node, "token.node.null");
        Objects.requireNonNull(token, "token.value.null");
        byte[] presented = hash(token);
        Instant now = clock.instant();
        Map.Entry<UUID, Entry> match = null;
        // Compare against every entry without stopping early, so the time taken does not reveal
        // which entry matched.
        for (Map.Entry<UUID, Entry> candidate : entries.entrySet()) {
            if (candidate.getValue().matches(presented)) {
                match = candidate;
            }
        }
        Arrays.fill(presented, (byte) 0);
        if (match == null) {
            return false;
        }
        Entry entry = match.getValue();
        if (entry.isExpired(now)) {
            entries.remove(match.getKey(), entry);
            return false;
        }
        if (!entry.node().equals(node)) {
            return false;
        }
        // remove(key, value) succeeds for exactly one concurrent caller.
        return entries.remove(match.getKey(), entry);
    }

    /**
     * Removes expired tokens.
     *
     * @return how many tokens were removed
     */
    public int purgeExpired() {
        Instant now = clock.instant();
        List<Map.Entry<UUID, Entry>> expired = new ArrayList<>();
        for (Map.Entry<UUID, Entry> candidate : entries.entrySet()) {
            if (candidate.getValue().isExpired(now)) {
                expired.add(candidate);
            }
        }
        int removed = 0;
        for (Map.Entry<UUID, Entry> candidate : expired) {
            if (entries.remove(candidate.getKey(), candidate.getValue())) {
                removed++;
            }
        }
        return removed;
    }

    private static byte[] hash(byte[] token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("token.hash.unavailable", failure);
        }
    }

    /**
     * A stored token: the node, the hash of the token and its expiry. Never the token itself. A class
     * rather than a record, because a record would expose the hash array and print it.
     */
    private static final class Entry {
        private final NodeName node;
        private final byte[] hash;
        private final Instant expiresAt;

        Entry(NodeName node, byte[] hash, Instant expiresAt) {
            this.node = node;
            this.hash = hash;
            this.expiresAt = expiresAt;
        }

        NodeName node() {
            return node;
        }

        boolean isExpired(Instant now) {
            return !now.isBefore(expiresAt);
        }

        /** Compares in constant time against a presented hash. */
        boolean matches(byte[] presentedHash) {
            return MessageDigest.isEqual(hash, presentedHash);
        }

        @Override
        public String toString() {
            return "Entry[node=" + node.value() + ", expiresAt=" + expiresAt + "]";
        }
    }
}
