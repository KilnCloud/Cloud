package at.kilncloud.master;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** A clock that tests can set and advance, so expiry logic is deterministic. */
public final class TestClock extends Clock {
    private final AtomicReference<Instant> now;

    /**
     * Creates a clock fixed at the given instant.
     *
     * @param start the initial time
     */
    public TestClock(Instant start) {
        this.now = new AtomicReference<>(start);
    }

    /**
     * Moves the clock forward.
     *
     * @param amount how far to advance
     */
    public void advance(Duration amount) {
        now.updateAndGet(current -> current.plus(amount));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now.get();
    }
}
