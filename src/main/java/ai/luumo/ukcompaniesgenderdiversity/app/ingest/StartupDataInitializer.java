package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import ai.luumo.ukcompaniesgenderdiversity.app.store.StoreSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class StartupDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupDataInitializer.class);

    private final StoreSnapshotService snapshotService;
    private final InMemoryCompanyStore inMemoryCompanyStore;
    private final DataRefreshService dataRefreshService;
    private final TaskExecutor taskExecutor;
    private final AppProperties appProperties;

    public StartupDataInitializer(
            StoreSnapshotService snapshotService,
            InMemoryCompanyStore inMemoryCompanyStore,
            DataRefreshService dataRefreshService,
            TaskExecutor taskExecutor,
            AppProperties appProperties
    ) {
        this.snapshotService = snapshotService;
        this.inMemoryCompanyStore = inMemoryCompanyStore;
        this.dataRefreshService = dataRefreshService;
        this.taskExecutor = taskExecutor;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Application startup data initialization started.");

        CompanyStoreSnapshot loadedSnapshot = snapshotService.loadSnapshotIfPresent().orElse(null);
        if (loadedSnapshot != null) {
            inMemoryCompanyStore.replace(loadedSnapshot);
            log.info("Data store loaded from on-disk snapshot.");
            if (loadedSnapshot.recentSubmissions().isEmpty()) {
                log.info("Loaded snapshot has no recent submissions; recompiling from local CSV files for compatibility.");
                try {
                    CompanyStoreSnapshot rebuilt = dataRefreshService.compileFromLocalFiles();
                    inMemoryCompanyStore.replace(rebuilt);
                    snapshotService.writeSnapshot(rebuilt);
                    log.info("Snapshot migration complete: recent submissions are now available.");
                } catch (RuntimeException ex) {
                    log.warn("Snapshot migration failed; continuing with loaded snapshot.", ex);
                }
            }
        } else {
            log.info("No on-disk snapshot found. Data store starts empty until refresh compiles source data.");
        }

        log.info("Current in-memory store state: {}", inMemoryCompanyStore.describeState());

        if (appProperties.startupRefreshEnabled()) {
            if (inMemoryCompanyStore.isEmpty()) {
                log.info("Startup refresh is enabled and store is empty; running refresh before accepting traffic.");
                try {
                    dataRefreshService.refreshFromSource();
                } catch (RuntimeException ex) {
                    log.error("Startup refresh failed; search will stay empty until a later refresh succeeds.", ex);
                }
            } else {
                log.info("Startup refresh is enabled; scheduling asynchronous data refresh.");
                taskExecutor.execute(dataRefreshService::refreshFromSource);
            }
        } else {
            log.info("Startup refresh is disabled; keeping current in-memory store state.");
        }
    }
}
