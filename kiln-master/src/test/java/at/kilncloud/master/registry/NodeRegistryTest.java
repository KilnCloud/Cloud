package at.kilncloud.master.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.master.TestClock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link NodeRegistry} and {@link NodeSnapshot}. */
final class NodeRegistryTest {
    private static final Instant START = Instant.parse("2026-10-14T12:00:00Z");
    private static final NodeName NODE = new NodeName("node-1");

    private final TestClock clock = new TestClock(START);
    private final NodeRegistry registry = new NodeRegistry(clock, Duration.ofSeconds(30));

    @Test
    @DisplayName("enroll merkt einen Node vor, revoke entfernt ihn wieder")
    void enrollAndRevoke() {
        assertFalse(registry.isEnrolled(NODE));
        registry.enroll(NODE);
        assertTrue(registry.isEnrolled(NODE));
        assertTrue(registry.revoke(NODE));
        assertFalse(registry.isEnrolled(NODE));
        assertFalse(registry.revoke(NODE));
    }

    @Test
    @DisplayName("enroll lässt den Verbindungszustand eines bereits aufgenommenen Nodes unberührt")
    void enroll_keepsConnectionOfEnrolledNode() {
        registry.enroll(NODE);
        registry.connect(NODE);
        registry.enroll(NODE);
        assertTrue(registry.isOnline(NODE));
    }

    @Test
    @DisplayName("connect verlangt ein vorheriges enroll")
    void connect_requiresEnrolment() {
        assertFalse(registry.connect(NODE));
        registry.enroll(NODE);
        assertTrue(registry.connect(NODE));
        assertFalse(registry.connect(new NodeName("other")));
    }

    @Test
    @DisplayName("heartbeat wird für einen verbundenen Node akzeptiert")
    void heartbeat_acceptedWhenConnected() {
        registry.enroll(NODE);
        registry.connect(NODE);
        assertEquals(HeartbeatResult.ACCEPTED, registry.heartbeat(NODE, 1, 2048));
        assertEquals(2048, registry.snapshot().get(0).memoryUsedMib());
    }

    @Test
    @DisplayName("heartbeat verlangt Enrollment und Verbindung")
    void heartbeat_requiresRegistration() {
        assertEquals(HeartbeatResult.NOT_REGISTERED, registry.heartbeat(NODE, 1, 0));
        registry.enroll(NODE);
        assertEquals(HeartbeatResult.NOT_REGISTERED, registry.heartbeat(NODE, 1, 0));
    }

    @Test
    @DisplayName("heartbeat weist eine wiederholte oder kleinere Sequenznummer ab")
    void heartbeat_rejectsReplay() {
        registry.enroll(NODE);
        registry.connect(NODE);
        assertEquals(HeartbeatResult.ACCEPTED, registry.heartbeat(NODE, 5, 0));
        assertEquals(HeartbeatResult.REPLAYED, registry.heartbeat(NODE, 5, 0));
        assertEquals(HeartbeatResult.REPLAYED, registry.heartbeat(NODE, 4, 0));
        assertEquals(HeartbeatResult.ACCEPTED, registry.heartbeat(NODE, 6, 0));
    }

    @Test
    @DisplayName("heartbeat weist negative Speicherwerte ab, ohne die Sequenz zu verbrauchen")
    void heartbeat_rejectsNegativeMemory() {
        registry.enroll(NODE);
        registry.connect(NODE);
        assertEquals(HeartbeatResult.INVALID, registry.heartbeat(NODE, 1, -1));
        assertEquals(HeartbeatResult.ACCEPTED, registry.heartbeat(NODE, 1, 10));
    }

    @Test
    @DisplayName("connect setzt die Sequenz einer neuen Verbindung zurück")
    void connect_resetsSequence() {
        registry.enroll(NODE);
        registry.connect(NODE);
        registry.heartbeat(NODE, 100, 0);
        registry.connect(NODE);
        assertEquals(HeartbeatResult.ACCEPTED, registry.heartbeat(NODE, 1, 0));
    }

    @Test
    @DisplayName("isOnline ist innerhalb der Frist wahr und danach falsch")
    void isOnline_followsHeartbeatTimeout() {
        registry.enroll(NODE);
        registry.connect(NODE);
        assertTrue(registry.isOnline(NODE));
        clock.advance(Duration.ofSeconds(29));
        assertTrue(registry.isOnline(NODE));
        clock.advance(Duration.ofSeconds(1));
        assertFalse(registry.isOnline(NODE));
        registry.heartbeat(NODE, 1, 0);
        assertTrue(registry.isOnline(NODE));
    }

    @Test
    @DisplayName("isOnline ist für unbekannte und nicht verbundene Nodes falsch")
    void isOnline_falseForUnknownAndUnconnected() {
        assertFalse(registry.isOnline(NODE));
        registry.enroll(NODE);
        assertFalse(registry.isOnline(NODE));
    }

    @Test
    @DisplayName("snapshot liefert alle Nodes nach Namen sortiert")
    void snapshot_sortedByName() {
        registry.enroll(new NodeName("zeta"));
        registry.enroll(new NodeName("alpha"));
        registry.connect(new NodeName("alpha"));
        List<NodeSnapshot> snapshot = registry.snapshot();
        assertEquals(
                List.of("alpha", "zeta"),
                snapshot.stream().map(s -> s.node().value()).toList());
        assertTrue(snapshot.get(0).online());
        assertEquals(Optional.of(START), snapshot.get(0).lastSeen());
        assertEquals(Optional.empty(), snapshot.get(1).lastSeen());
        assertFalse(snapshot.get(1).online());
    }

    @Test
    @DisplayName("Der Konstruktor lehnt ungültige Argumente ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void constructor_rejectsInvalidArguments() {
        assertThrows(NullPointerException.class, () -> new NodeRegistry(null, Duration.ofSeconds(1)));
        assertThrows(NullPointerException.class, () -> new NodeRegistry(clock, null));
        assertThrows(IllegalArgumentException.class, () -> new NodeRegistry(clock, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new NodeRegistry(clock, Duration.ofSeconds(-1)));
    }

    @Test
    @DisplayName("Die Methoden lehnen null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void methods_rejectNull() {
        assertThrows(NullPointerException.class, () -> registry.enroll(null));
        assertThrows(NullPointerException.class, () -> registry.isEnrolled(null));
        assertThrows(NullPointerException.class, () -> registry.revoke(null));
        assertThrows(NullPointerException.class, () -> registry.connect(null));
        assertThrows(NullPointerException.class, () -> registry.heartbeat(null, 1, 1));
        assertThrows(NullPointerException.class, () -> registry.isOnline(null));
        assertThrows(NullPointerException.class, () -> new NodeSnapshot(null, true, 0, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new NodeSnapshot(NODE, true, 0, null));
    }
}
