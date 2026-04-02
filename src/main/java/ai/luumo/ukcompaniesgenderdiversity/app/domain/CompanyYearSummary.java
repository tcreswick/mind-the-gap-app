package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.time.Instant;

public record CompanyYearSummary(
        int reportingYear,
        String employerName,
        // Averaged overall split (used for headline chart)
        Double femaleOverallPercent,
        Double maleOverallPercent,
        // Hourly pay gap (positive = women earn less than men)
        Double diffMeanHourlyPercent,
        Double diffMedianHourlyPercent,
        // Bonus pay gap
        Double diffMeanBonusPercent,
        Double diffMedianBonusPercent,
        // Bonus participation (% of each gender who received a bonus)
        Double maleBonusPercent,
        Double femaleBonusPercent,
        // Pay quartiles (% of each quartile that is male/female)
        Double maleLowerQuartile,
        Double femaleLowerQuartile,
        Double maleLowerMiddleQuartile,
        Double femaleLowerMiddleQuartile,
        Double maleUpperMiddleQuartile,
        Double femaleUpperMiddleQuartile,
        Double maleTopQuartile,
        Double femaleTopQuartile,
        // Submission timing
        boolean submittedAfterDeadline,
        Instant submittedAt
) {
    public CompanyYearSummary(
            int reportingYear,
            String employerName,
            Double femaleOverallPercent,
            Double maleOverallPercent
    ) {
        this(
                reportingYear,
                employerName,
                femaleOverallPercent,
                maleOverallPercent,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null
        );
    }
}
