package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.GenderSplitCalculator;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.StoreMetadata;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.YearlySubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CompanyHistoryCompiler {
    private static final Logger log = LoggerFactory.getLogger(CompanyHistoryCompiler.class);

    public CompanyStoreSnapshot compile(Map<Integer, List<YearlySubmission>> byYearSubmissions) {
        List<YearlySubmission> all = byYearSubmissions.values().stream()
                .flatMap(List::stream)
                .toList();
        Map<String, List<YearlySubmission>> grouped = all.stream()
                .collect(Collectors.groupingBy(YearlySubmission::employerId));

        List<CompanyHistory> companies = grouped.values().stream()
                .map(this::toCompanyHistory)
                .sorted(Comparator.comparing(CompanyHistory::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        StoreMetadata metadata = new StoreMetadata(
                Instant.now(),
                byYearSubmissions.keySet().stream().sorted().toList(),
                companies.size(),
                all.size()
        );
        log.info(
                "Compiled company history snapshot: companies={}, submissions={}, years={}.",
                metadata.companyCount(),
                metadata.submissionCount(),
                metadata.sourceYearsLoaded()
        );
        return new CompanyStoreSnapshot(metadata, companies);
    }

    private CompanyHistory toCompanyHistory(List<YearlySubmission> submissions) {
        List<YearlySubmission> sorted = submissions.stream()
                .sorted(Comparator.comparingInt(YearlySubmission::reportingYear))
                .toList();
        YearlySubmission latest = sorted.get(sorted.size() - 1);

        Set<String> aliases = sorted.stream()
                .flatMap(s -> List.of(s.employerName(), s.currentName()).stream())
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CompanyYearSummary> summaries = sorted.stream()
                .map(this::toYearSummary)
                .toList();

        return new CompanyHistory(
                latest.employerId(),
                pickDisplayName(latest),
                latest.companyNumber(),
                latest.employerSize(),
                aliases,
                summaries.stream().map(CompanyYearSummary::reportingYear).toList(),
                summaries
        );
    }

    private CompanyYearSummary toYearSummary(YearlySubmission s) {
        Double femaleAverage = GenderSplitCalculator.average(
                s.femaleLowerQuartile(),
                s.femaleLowerMiddleQuartile(),
                s.femaleUpperMiddleQuartile(),
                s.femaleTopQuartile()
        );
        Double maleAverage = GenderSplitCalculator.average(
                s.maleLowerQuartile(),
                s.maleLowerMiddleQuartile(),
                s.maleUpperMiddleQuartile(),
                s.maleTopQuartile()
        );

        return new CompanyYearSummary(
                s.reportingYear(),
                pickDisplayName(s),
                femaleAverage,
                maleAverage,
                s.diffMeanHourlyPercent(),
                s.diffMedianHourlyPercent(),
                s.diffMeanBonusPercent(),
                s.diffMedianBonusPercent(),
                s.maleBonusPercent(),
                s.femaleBonusPercent(),
                s.maleLowerQuartile(),
                s.femaleLowerQuartile(),
                s.maleLowerMiddleQuartile(),
                s.femaleLowerMiddleQuartile(),
                s.maleUpperMiddleQuartile(),
                s.femaleUpperMiddleQuartile(),
                s.maleTopQuartile(),
                s.femaleTopQuartile(),
                s.submittedAfterDeadline()
        );
    }

    private String pickDisplayName(YearlySubmission submission) {
        if (submission.currentName() != null && !submission.currentName().isBlank()) {
            return submission.currentName().trim();
        }
        if (submission.employerName() != null && !submission.employerName().isBlank()) {
            return submission.employerName().trim();
        }
        return "Unknown employer";
    }
}
