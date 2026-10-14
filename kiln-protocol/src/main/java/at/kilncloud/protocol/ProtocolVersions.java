package at.kilncloud.protocol;

import at.kilncloud.protocol.v1.ProtocolVersion;
import java.util.Objects;

/**
 * Protocol version of this build and the compatibility rule between peers (ADR 0003).
 *
 * <p>Two peers can talk when their major versions are equal and non-zero. Minor versions only add
 * optional fields, so a lower or higher minor version is accepted.
 */
public final class ProtocolVersions {
    /** The version this build speaks. */
    private static final ProtocolVersion CURRENT =
            ProtocolVersion.newBuilder().setMajor(1).setMinor(0).build();

    /** Prevents construction of this stateless utility class. */
    private ProtocolVersions() {}

    /**
     * Returns the protocol version of this build.
     *
     * @return the immutable current version
     */
    public static ProtocolVersion current() {
        return CURRENT;
    }

    /**
     * Decides whether this build can talk to a peer.
     *
     * <p>Major version {@code 0} is never compatible: it is what an unset field decodes to, so a peer
     * that sends no version is refused instead of being treated as "any version".
     *
     * @param remote the version announced by the peer
     * @return {@code true} if the major versions are equal and not zero
     * @throws NullPointerException if {@code remote} is {@code null}
     */
    public static boolean isCompatible(ProtocolVersion remote) {
        Objects.requireNonNull(remote, "protocol.version.null");
        return remote.getMajor() != 0 && remote.getMajor() == CURRENT.getMajor();
    }
}
