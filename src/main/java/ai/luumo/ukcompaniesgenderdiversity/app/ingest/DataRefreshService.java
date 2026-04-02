package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.YearlySubmission;
import ai.luumo.ukcompaniesgenderdiversity.app.store.CompanyHistoryCompiler;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import ai.luumo.ukcompaniesgenderdiversity.app.store.StoreSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DataRefreshService {
    private static final Logger log = LoggerFactory.getLogger(DataRefreshService.class);

    private final GovUkDownloadClient downloadClient;
    private final LocalCsvRepository localCsvRepository;
    private final CsvSubmissionParser csvSubmissionParser;
    private final CompanyHistoryCompiler companyHistoryCompiler;
    private final InMemoryCompanyStore inMemoryCompanyStore;
    private final StoreSnapshotService storeSnapshotService;
    private final ReentrantLock refreshLock = new ReentrantLock();

    public DataRefreshService(
            GovUkDownloadClient downloadClient,
            LocalCsvRepository localCsvRepository,
            CsvSubmissionParser csvSubmissionParser,
            CompanyHistoryCompiler companyHistoryCompiler,
            InMemoryCompanyStore inMemoryCompanyStore,
            StoreSnapshotService storeSnapshotService
    ) {
        this.downloadClient = downloadClient;
        this.localCsvRepository = localCsvRepository;
        this.csvSubmissionParser = csvSubmissionParser;
        this.companyHistoryCompiler = companyHistoryCompiler;
        this.inMemoryCompanyStore = inMemoryCompanyStore;
        this.storeSnapshotService = storeSnapshotService;
    }

    public void refreshFromSource() {
        if (!refreshLock.tryLock()) {
            log.info("Skipping refresh run because another refresh is in progress.");
            return;
        }

        try {
            log.info("Starting data refresh from upstream source.");
            log.info("In-memory store before refresh: {}", inMemoryCompanyStore.describeState());

            boolean changed = false;
            List<Integer> discoveredYears = new ArrayList<>();
            int year = downloadClient.sourceStartYear();
            while (true) {
                URI sourceUrl = downloadClient.sourceCsvUrlForYear(year);
                log.info("Fetching CSV for reporting year {} from {}.", year, sourceUrl);
                byte[] bytes = downloadClient.downloadCsvIfPresent(sourceUrl);
                if (bytes == null) {
                    log.info("Stopping source scan at year {} after receiving 404 from {}.", year, sourceUrl);
                    break;
                }
                discoveredYears.add(year);
                boolean yearChanged = localCsvRepository.saveIfChanged(year, bytes);
                if (yearChanged) {
                    log.info("Local CSV updated for reporting year {}.", year);
                } else {
                    log.info("Local CSV unchanged for reporting year {}.", year);
                }
                changed = changed || yearChanged;
                year++;
            }
            log.info("Discovered {} CSV source files to inspect: years={}.", discoveredYears.size(), discoveredYears);

            if (changed || inMemoryCompanyStore.isEmpty()) {
                if (inMemoryCompanyStore.isEmpty()) {
                    log.info("Compiling snapshot because in-memory store is empty.");
                } else {
                    log.info("Compiling snapshot because at least one source CSV changed.");
                }
                CompanyStoreSnapshot snapshot = compileFromLocalFiles();
                inMemoryCompanyStore.replace(snapshot);
                storeSnapshotService.writeSnapshot(snapshot);
                log.info(
                        "Store refreshed from source: {} companies across {} submissions.",
                        snapshot.metadata().companyCount(),
                        snapshot.metadata().submissionCount()
                );
            } else {
                log.info("No source data changes detected; store left unchanged.");
            }
            log.info("In-memory store after refresh: {}", inMemoryCompanyStore.describeState());
        } finally {
            refreshLock.unlock();
        }
    }

    public CompanyStoreSnapshot compileFromLocalFiles() {
        Map<Integer, List<YearlySubmission>> byYear = new HashMap<>();
        Map<Integer, Path> localFiles = localCsvRepository.listStoredCsvFiles();
        log.info("Compiling store from {} local CSV files: years={}.", localFiles.size(), localFiles.keySet());
        for (Map.Entry<Integer, Path> entry : localFiles.entrySet()) {
            log.info("Parsing local CSV for year {} from {}.", entry.getKey(), entry.getValue());
            byYear.put(entry.getKey(), csvSubmissionParser.parse(entry.getValue(), entry.getKey()));
        }
        CompanyStoreSnapshot snapshot = companyHistoryCompiler.compile(byYear);
        log.info(
                "Compiled snapshot in memory: companies={}, submissions={}, years={}.",
                snapshot.metadata().companyCount(),
                snapshot.metadata().submissionCount(),
                snapshot.metadata().sourceYearsLoaded()
        );
        return snapshot;
    }
}
