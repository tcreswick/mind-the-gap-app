package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.time.LocalDate;

public record DailySubmissionCount(
        LocalDate day,
        int submissionCount
) {
}
