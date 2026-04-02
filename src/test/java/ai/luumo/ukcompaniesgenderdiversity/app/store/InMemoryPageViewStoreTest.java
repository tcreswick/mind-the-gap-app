package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyPageViewSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPageViewStoreTest {
    private final InMemoryPageViewStore store = new InMemoryPageViewStore();

    @Test
    void recordAndCountPrunesOlderThanThirtyDays() {
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        store.recordView("EMP-1", now.minusSeconds(31L * 24 * 3600));
        store.recordView("EMP-1", now.minusSeconds(10L * 24 * 3600));

        long count = store.countViewsLast30Days("EMP-1", now);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void topViewedReturnsDescendingCounts() {
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        store.recordView("EMP-1", now.minusSeconds(60));
        store.recordView("EMP-1", now.minusSeconds(30));
        store.recordView("EMP-2", now.minusSeconds(20));

        assertThat(store.topViewedLast30Days(10, now))
                .extracting(item -> item.employerId() + ":" + item.viewCount())
                .containsExactly("EMP-1:2", "EMP-2:1");
    }

    @Test
    void replaceLoadsSnapshotAndPrunesExpiredEntries() {
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        CompanyPageViewSnapshot snapshot = new CompanyPageViewSnapshot(Map.of(
                "EMP-1", List.of(now.minusSeconds(31L * 24 * 3600), now.minusSeconds(100)),
                "EMP-2", List.of(now.minusSeconds(50))
        ));

        store.replace(snapshot, now);

        assertThat(store.countViewsLast30Days("EMP-1", now)).isEqualTo(1);
        assertThat(store.countViewsLast30Days("EMP-2", now)).isEqualTo(1);
    }
}
