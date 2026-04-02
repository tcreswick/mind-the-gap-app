package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.time.Instant;

public record YearlySubmission(
        int reportingYear,
        String employerId,
        String employerName,
        String currentName,
        String companyNumber,
        String employerSize,
        Double diffMeanHourlyPercent,
        Double diffMedianHourlyPercent,
        Double diffMeanBonusPercent,
        Double diffMedianBonusPercent,
        Double maleBonusPercent,
        Double femaleBonusPercent,
        Double maleLowerQuartile,
        Double femaleLowerQuartile,
        Double maleLowerMiddleQuartile,
        Double femaleLowerMiddleQuartile,
        Double maleUpperMiddleQuartile,
        Double femaleUpperMiddleQuartile,
        Double maleTopQuartile,
        Double femaleTopQuartile,
        boolean submittedAfterDeadline,
        Instant submittedAt
) {
}
