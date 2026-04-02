package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.time.Instant;
import java.util.List;

public record StoreMetadata(
        Instant lastUpdatedAt,
        List<Integer> sourceYearsLoaded,
        int companyCount,
        int submissionCount
) {
}
