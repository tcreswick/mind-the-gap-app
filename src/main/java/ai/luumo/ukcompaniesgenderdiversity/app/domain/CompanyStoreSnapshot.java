package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.util.List;

public record CompanyStoreSnapshot(
        StoreMetadata metadata,
        List<CompanyHistory> companies
) {
    public static CompanyStoreSnapshot empty() {
        return new CompanyStoreSnapshot(new StoreMetadata(null, List.of(), 0, 0), List.of());
    }
}
