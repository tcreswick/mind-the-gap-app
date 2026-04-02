package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.time.Instant;

public record RecentSubmissionItem(
        String employerId,
        String displayName,
        int reportingYear,
        Instant submittedAt
) {
}
