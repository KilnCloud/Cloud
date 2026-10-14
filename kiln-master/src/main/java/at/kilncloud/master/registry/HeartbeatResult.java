package at.kilncloud.master.registry;

/** Outcome of reporting a heartbeat for a node. */
public enum HeartbeatResult {
    /** The heartbeat was recorded. */
    ACCEPTED,
    /** The node is not enrolled or has no active connection; it must register first. */
    NOT_REGISTERED,
    /** The sequence number is not larger than the last one, which indicates a replayed heartbeat. */
    REPLAYED,
    /** The reported values are invalid, for example a negative memory figure. */
    INVALID
}
