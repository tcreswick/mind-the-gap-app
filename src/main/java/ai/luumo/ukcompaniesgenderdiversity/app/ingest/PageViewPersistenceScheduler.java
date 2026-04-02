package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryPageViewStore;
import ai.luumo.ukcompaniesgenderdiversity.app.store.PageViewSnapshotService;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PageViewPersistenceScheduler {
    private final InMemoryPageViewStore inMemoryPageViewStore;
    private final PageViewSnapshotService pageViewSnapshotService;

    public PageViewPersistenceScheduler(
            InMemoryPageViewStore inMemoryPageViewStore,
            PageViewSnapshotService pageViewSnapshotService
    ) {
        this.inMemoryPageViewStore = inMemoryPageViewStore;
        this.pageViewSnapshotService = pageViewSnapshotService;
    }

    @Scheduled(fixedDelayString = "${app.page-views-persist-interval-ms}")
    public void persistPageViews() {
        pageViewSnapshotService.writeSnapshot(inMemoryPageViewStore.snapshot(Instant.now()));
    }

    @PreDestroy
    public void flushOnShutdown() {
        pageViewSnapshotService.writeSnapshot(inMemoryPageViewStore.snapshot(Instant.now()));
    }
}
