package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.StoreStatisticsSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.web.SearchSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryCompanyStore {
    private static final Logger log = LoggerFactory.getLogger(InMemoryCompanyStore.class);

    private final StoreStatisticsCompiler storeStatisticsCompiler;
    private final AtomicReference<CompanyStoreSnapshot> snapshotRef = new AtomicReference<>(CompanyStoreSnapshot.empty());
    private final AtomicReference<StoreStatisticsSnapshot> statisticsRef = new AtomicReference<>(StoreStatisticsSnapshot.empty());
    private final Map<String, CompanyHistory> byEmployerId = new ConcurrentHashMap<>();

    public InMemoryCompanyStore(StoreStatisticsCompiler storeStatisticsCompiler) {
        this.storeStatisticsCompiler = storeStatisticsCompiler;
    }

    public synchronized void replace(CompanyStoreSnapshot snapshot) {
        byEmployerId.clear();
        for (CompanyHistory company : snapshot.companies()) {
            byEmployerId.put(company.employerId(), company);
        }
        snapshotRef.set(snapshot);
        statisticsRef.set(storeStatisticsCompiler.compile(byEmployerId.values(), Instant.now()));
        log.info("In-memory store replaced: {}", describeState());
    }

    public CompanyStoreSnapshot snapshot() {
        return snapshotRef.get();
    }

    public boolean isEmpty() {
        return byEmployerId.isEmpty();
    }

    public StoreStatisticsSnapshot statisticsSnapshot() {
        return statisticsRef.get();
    }

    public String describeState() {
        CompanyStoreSnapshot snapshot = snapshotRef.get();
        return "companies=%d, submissions=%d, years=%s, lastUpdatedAt=%s".formatted(
                snapshot.metadata().companyCount(),
                snapshot.metadata().submissionCount(),
                snapshot.metadata().sourceYearsLoaded(),
                snapshot.metadata().lastUpdatedAt()
        );
    }

    public Optional<CompanyHistory> getByEmployerId(String employerId) {
        return Optional.ofNullable(byEmployerId.get(employerId));
    }

    public List<SearchSuggestion> searchByName(String query, int limit) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<SearchSuggestion> results = new ArrayList<>();
        for (CompanyHistory company : byEmployerId.values()) {
            boolean matched = company.displayName().toLowerCase().contains(normalized)
                    || company.nameAliases().stream().anyMatch(alias -> alias.toLowerCase().contains(normalized));
            if (matched) {
                results.add(new SearchSuggestion(company.employerId(), company.displayName()));
            }
        }

        return results.stream()
                .sorted(Comparator.comparing(SearchSuggestion::displayName, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .toList();
    }
}
