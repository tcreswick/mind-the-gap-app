package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyPageViewSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryPageViewStore;
import ai.luumo.ukcompaniesgenderdiversity.app.store.PageViewSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Order(20)
public class StartupPageViewInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupPageViewInitializer.class);

    private final PageViewSnapshotService pageViewSnapshotService;
    private final InMemoryPageViewStore inMemoryPageViewStore;

    public StartupPageViewInitializer(
            PageViewSnapshotService pageViewSnapshotService,
            InMemoryPageViewStore inMemoryPageViewStore
    ) {
        this.pageViewSnapshotService = pageViewSnapshotService;
        this.inMemoryPageViewStore = inMemoryPageViewStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        CompanyPageViewSnapshot snapshot = pageViewSnapshotService.loadSnapshotIfPresent().orElse(null);
        if (snapshot == null) {
            log.info("No on-disk page-view snapshot found.");
            return;
        }
        inMemoryPageViewStore.replace(snapshot, Instant.now());
        int companies = inMemoryPageViewStore.topViewedLast30Days(Integer.MAX_VALUE, Instant.now()).size();
        log.info("Page-view store loaded from on-disk snapshot: companiesWithViews={}.", companies);
    }
}
