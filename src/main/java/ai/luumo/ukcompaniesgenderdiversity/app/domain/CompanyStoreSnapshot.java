package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.util.List;

public record CompanyStoreSnapshot(
        StoreMetadata metadata,
        List<CompanyHistory> companies,
        List<RecentSubmissionItem> recentSubmissions
) {
    public CompanyStoreSnapshot {
        if (companies == null) {
            companies = List.of();
        }
        if (recentSubmissions == null) {
            recentSubmissions = List.of();
        }
    }

    public static CompanyStoreSnapshot empty() {
        return new CompanyStoreSnapshot(new StoreMetadata(null, List.of(), 0, 0), List.of(), List.of());
    }
}
