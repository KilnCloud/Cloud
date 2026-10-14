package at.kilncloud.setup.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.NodeName;
import at.kilncloud.setup.TestClock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link JoinTokens}. */
final class JoinTokensTest {
    private static final NodeName NODE = new NodeName("node-1");
    private static final Duration TTL = Duration.ofMinutes(15);

    private final TestClock clock = new TestClock(Instant.parse("2026-10-14T12:00:00Z"));
    private final JoinTokens tokens = new JoinTokens(clock);

    @Test
    @DisplayName("issue liefert ein Token mit 32 Zufallsbytes, jedes Mal ein anderes")
    void issue_returnsDistinctRandomTokens() {
        byte[] first = tokens.issue(NODE, TTL);
        byte[] second = tokens.issue(NODE, TTL);
        assertEquals(32, first.length);
        assertFalse(java.util.Arrays.equals(first, second));
    }

    @Test
    @DisplayName("redeem akzeptiert ein gültiges Token für den richtigen Node")
    void redeem_acceptsValidToken() {
        byte[] token = tokens.issue(NODE, TTL);
        assertTrue(tokens.redeem(NODE, token));
    }

    @Test
    @DisplayName("redeem lässt ein Token nur einmal zu")
    void redeem_worksOnlyOnce() {
        byte[] token = tokens.issue(NODE, TTL);
        assertTrue(tokens.redeem(NODE, token));
        assertFalse(tokens.redeem(NODE, token));
    }

    @Test
    @DisplayName("redeem lehnt ein unbekanntes Token ab")
    void redeem_rejectsUnknownToken() {
        tokens.issue(NODE, TTL);
        assertFalse(tokens.redeem(NODE, new byte[32]));
        assertFalse(tokens.redeem(NODE, new byte[0]));
    }

    @Test
    @DisplayName("redeem lehnt ein Token für einen anderen Node ab und verbraucht es nicht")
    void redeem_wrongNodeDoesNotConsumeToken() {
        byte[] token = tokens.issue(NODE, TTL);
        assertFalse(tokens.redeem(new NodeName("node-2"), token));
        assertTrue(tokens.redeem(NODE, token));
    }

    @Test
    @DisplayName("redeem lehnt ein abgelaufenes Token ab")
    void redeem_rejectsExpiredToken() {
        byte[] token = tokens.issue(NODE, TTL);
        clock.advance(TTL);
        assertFalse(tokens.redeem(NODE, token));
    }

    @Test
    @DisplayName("redeem akzeptiert ein Token bis kurz vor dem Ablauf")
    void redeem_acceptsTokenJustBeforeExpiry() {
        byte[] token = tokens.issue(NODE, TTL);
        clock.advance(TTL.minusSeconds(1));
        assertTrue(tokens.redeem(NODE, token));
    }

    @Test
    @DisplayName("redeem lässt bei zwei gleichzeitigen Versuchen nur einen gewinnen")
    void redeem_onlyOneConcurrentCallerWins() throws Exception {
        byte[] token = tokens.issue(NODE, TTL);
        int callers = 8;
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return tokens.redeem(NODE, token);
                }));
            }
            start.countDown();
            int winners = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    winners++;
                }
            }
            assertEquals(1, winners);
        }
    }

    @Test
    @DisplayName("issue lehnt ungültige Lebensdauern ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void issue_rejectsInvalidTtl() {
        assertThrows(IllegalArgumentException.class, () -> tokens.issue(NODE, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> tokens.issue(NODE, Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> tokens.issue(NODE, JoinTokens.MAX_TTL.plusSeconds(1)));
        assertTrue(tokens.issue(NODE, JoinTokens.MAX_TTL).length > 0);
        assertThrows(NullPointerException.class, () -> tokens.issue(null, TTL));
        assertThrows(NullPointerException.class, () -> tokens.issue(NODE, null));
    }

    @Test
    @DisplayName("issue begrenzt die Zahl offener Tokens")
    void issue_limitsOutstandingTokens() {
        for (int i = 0; i < JoinTokens.MAX_OUTSTANDING; i++) {
            tokens.issue(NODE, TTL);
        }
        assertThrows(IllegalStateException.class, () -> tokens.issue(NODE, TTL));
    }

    @Test
    @DisplayName("issue räumt abgelaufene Tokens auf, bevor es das Limit prüft")
    void issue_purgesExpiredBeforeCheckingLimit() {
        for (int i = 0; i < JoinTokens.MAX_OUTSTANDING; i++) {
            tokens.issue(NODE, TTL);
        }
        clock.advance(TTL);
        assertTrue(tokens.issue(NODE, TTL).length > 0);
    }

    @Test
    @DisplayName("purgeExpired entfernt nur abgelaufene Tokens und zählt sie")
    void purgeExpired_removesOnlyExpiredTokens() {
        byte[] shortLived = tokens.issue(NODE, Duration.ofMinutes(1));
        byte[] longLived = tokens.issue(NODE, Duration.ofHours(1));
        clock.advance(Duration.ofMinutes(5));
        assertEquals(1, tokens.purgeExpired());
        assertEquals(0, tokens.purgeExpired());
        assertFalse(tokens.redeem(NODE, shortLived));
        assertTrue(tokens.redeem(NODE, longLived));
    }

    @Test
    @DisplayName("Die Methoden lehnen null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void methods_rejectNull() {
        assertThrows(NullPointerException.class, () -> new JoinTokens(null));
        assertThrows(NullPointerException.class, () -> tokens.redeem(null, new byte[32]));
        assertThrows(NullPointerException.class, () -> tokens.redeem(NODE, null));
    }
}
