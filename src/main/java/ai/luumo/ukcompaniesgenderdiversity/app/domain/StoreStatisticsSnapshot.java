package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.util.List;

public record StoreStatisticsSnapshot(
        List<DailySubmissionCount> submissionsPerDayLast30Days
) {
    public StoreStatisticsSnapshot {
        if (submissionsPerDayLast30Days == null) {
            submissionsPerDayLast30Days = List.of();
        }
    }

    public static StoreStatisticsSnapshot empty() {
        return new StoreStatisticsSnapshot(List.of());
    }
}
