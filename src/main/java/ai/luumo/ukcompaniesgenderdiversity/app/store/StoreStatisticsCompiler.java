package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.DailySubmissionCount;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.StoreStatisticsSnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StoreStatisticsCompiler {
    private static final int ROLLING_DAYS = 30;

    public StoreStatisticsSnapshot compile(Collection<CompanyHistory> companies, Instant now) {
        LocalDate todayUtc = now.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate startDay = todayUtc.minusDays(ROLLING_DAYS - 1L);

        Map<LocalDate, Integer> countsByDay = new LinkedHashMap<>();
        for (int i = 0; i < ROLLING_DAYS; i++) {
            countsByDay.put(startDay.plusDays(i), 0);
        }

        for (CompanyHistory company : companies) {
            for (CompanyYearSummary summary : company.yearlySummaries()) {
                Instant submittedAt = summary.submittedAt();
                if (submittedAt == null) {
                    continue;
                }
                LocalDate day = submittedAt.atZone(ZoneOffset.UTC).toLocalDate();
                Integer currentCount = countsByDay.get(day);
                if (currentCount != null) {
                    countsByDay.put(day, currentCount + 1);
                }
            }
        }

        List<DailySubmissionCount> dailyCounts = new ArrayList<>(ROLLING_DAYS);
        for (Map.Entry<LocalDate, Integer> entry : countsByDay.entrySet()) {
            dailyCounts.add(new DailySubmissionCount(entry.getKey(), entry.getValue()));
        }
        return new StoreStatisticsSnapshot(dailyCounts);
    }
}
