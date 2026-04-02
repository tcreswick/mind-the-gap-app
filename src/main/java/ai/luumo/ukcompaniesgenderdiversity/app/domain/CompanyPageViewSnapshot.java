package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CompanyPageViewSnapshot(
        Map<String, List<Instant>> viewsByEmployerId
) {
    public static CompanyPageViewSnapshot empty() {
        return new CompanyPageViewSnapshot(Map.of());
    }
}
