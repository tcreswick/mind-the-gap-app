package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyPageViewSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyViewCount;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryPageViewStore {
    private static final Duration WINDOW = Duration.ofDays(30);

    private final Map<String, ArrayDeque<Instant>> viewsByEmployerId = new HashMap<>();

    public synchronized void recordView(String employerId, Instant viewedAt) {
        if (employerId == null || employerId.isBlank() || viewedAt == null) {
            return;
        }
        ArrayDeque<Instant> views = viewsByEmployerId.computeIfAbsent(employerId, ignored -> new ArrayDeque<>());
        views.addLast(viewedAt);
        pruneDeque(views, viewedAt.minus(WINDOW));
    }

    public synchronized long countViewsLast30Days(String employerId, Instant now) {
        ArrayDeque<Instant> views = viewsByEmployerId.get(employerId);
        if (views == null || now == null) {
            return 0;
        }
        pruneDeque(views, now.minus(WINDOW));
        if (views.isEmpty()) {
            viewsByEmployerId.remove(employerId);
            return 0;
        }
        return views.size();
    }

    public synchronized List<CompanyViewCount> topViewedLast30Days(int limit, Instant now) {
        if (limit <= 0 || now == null) {
            return List.of();
        }
        return viewedLast30Days(now).stream()
                .limit(limit)
                .toList();
    }

    public synchronized List<CompanyViewCount> viewedLast30Days(Instant now) {
        if (now == null) {
            return List.of();
        }
        Instant cutoff = now.minus(WINDOW);
        List<CompanyViewCount> results = new ArrayList<>();
        List<String> emptyKeys = new ArrayList<>();
        for (Map.Entry<String, ArrayDeque<Instant>> entry : viewsByEmployerId.entrySet()) {
            ArrayDeque<Instant> views = entry.getValue();
            pruneDeque(views, cutoff);
            if (views.isEmpty()) {
                emptyKeys.add(entry.getKey());
                continue;
            }
            results.add(new CompanyViewCount(entry.getKey(), views.size()));
        }
        for (String key : emptyKeys) {
            viewsByEmployerId.remove(key);
        }
        return results.stream()
                .sorted(Comparator.comparingLong(CompanyViewCount::viewCount).reversed()
                        .thenComparing(CompanyViewCount::employerId))
                .toList();
    }

    public synchronized long totalViewsLast30Days(Instant now) {
        if (now == null) {
            return 0;
        }
        Instant cutoff = now.minus(WINDOW);
        long total = 0;
        List<String> emptyKeys = new ArrayList<>();
        for (Map.Entry<String, ArrayDeque<Instant>> entry : viewsByEmployerId.entrySet()) {
            ArrayDeque<Instant> views = entry.getValue();
            pruneDeque(views, cutoff);
            if (views.isEmpty()) {
                emptyKeys.add(entry.getKey());
                continue;
            }
            total += views.size();
        }
        for (String key : emptyKeys) {
            viewsByEmployerId.remove(key);
        }
        return total;
    }

    public synchronized int companiesWithViewsLast30Days(Instant now) {
        if (now == null) {
            return 0;
        }
        Instant cutoff = now.minus(WINDOW);
        int companies = 0;
        List<String> emptyKeys = new ArrayList<>();
        for (Map.Entry<String, ArrayDeque<Instant>> entry : viewsByEmployerId.entrySet()) {
            ArrayDeque<Instant> views = entry.getValue();
            pruneDeque(views, cutoff);
            if (views.isEmpty()) {
                emptyKeys.add(entry.getKey());
                continue;
            }
            companies++;
        }
        for (String key : emptyKeys) {
            viewsByEmployerId.remove(key);
        }
        return companies;
    }

    public synchronized CompanyPageViewSnapshot snapshot(Instant now) {
        if (now == null) {
            return CompanyPageViewSnapshot.empty();
        }
        Instant cutoff = now.minus(WINDOW);
        Map<String, List<Instant>> snapshot = new HashMap<>();
        List<String> emptyKeys = new ArrayList<>();
        for (Map.Entry<String, ArrayDeque<Instant>> entry : viewsByEmployerId.entrySet()) {
            ArrayDeque<Instant> views = entry.getValue();
            pruneDeque(views, cutoff);
            if (views.isEmpty()) {
                emptyKeys.add(entry.getKey());
                continue;
            }
            snapshot.put(entry.getKey(), List.copyOf(views));
        }
        for (String key : emptyKeys) {
            viewsByEmployerId.remove(key);
        }
        return new CompanyPageViewSnapshot(Map.copyOf(snapshot));
    }

    public synchronized void replace(CompanyPageViewSnapshot snapshot, Instant now) {
        viewsByEmployerId.clear();
        if (snapshot == null || snapshot.viewsByEmployerId() == null || now == null) {
            return;
        }
        Instant cutoff = now.minus(WINDOW);
        for (Map.Entry<String, List<Instant>> entry : snapshot.viewsByEmployerId().entrySet()) {
            String employerId = entry.getKey();
            List<Instant> views = entry.getValue();
            if (employerId == null || employerId.isBlank() || views == null || views.isEmpty()) {
                continue;
            }
            ArrayDeque<Instant> filtered = new ArrayDeque<>();
            for (Instant viewedAt : views) {
                if (viewedAt != null && !viewedAt.isBefore(cutoff)) {
                    filtered.addLast(viewedAt);
                }
            }
            if (!filtered.isEmpty()) {
                viewsByEmployerId.put(employerId, filtered);
            }
        }
    }

    private static void pruneDeque(ArrayDeque<Instant> views, Instant cutoff) {
        while (!views.isEmpty()) {
            Instant first = views.peekFirst();
            if (first == null || first.isBefore(cutoff)) {
                views.pollFirst();
            } else {
                return;
            }
        }
    }
}
