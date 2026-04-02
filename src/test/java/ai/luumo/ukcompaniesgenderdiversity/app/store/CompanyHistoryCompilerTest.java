package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.YearlySubmission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyHistoryCompilerTest {
    private final CompanyHistoryCompiler compiler = new CompanyHistoryCompiler();

    @Test
    void compileGroupsByEmployerIdAcrossYears() {
        YearlySubmission y2022 = new YearlySubmission(
                2022, "EMP-1", "Example Ltd", "Example Ltd", "12345678", "250 to 499",
                60.0, 40.0, 60.0, 40.0, 55.0, 45.0,
                52.0, 48.0, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0,
                false
        );
        YearlySubmission y2023 = new YearlySubmission(
                2023, "EMP-1", "Example Ltd", "Example Holdings Ltd", "12345678", "500 to 999",
                58.0, 42.0, 59.0, 41.0, 56.0, 44.0,
                53.0, 47.0, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0,
                false
        );

        CompanyStoreSnapshot snapshot = compiler.compile(Map.of(
                2022, List.of(y2022),
                2023, List.of(y2023)
        ));

        assertThat(snapshot.companies()).hasSize(1);
        assertThat(snapshot.companies().get(0).submittedYears()).containsExactly(2022, 2023);
        assertThat(snapshot.metadata().submissionCount()).isEqualTo(2);
    }

    @Test
    void yearlySummaryUsesQuartileSplitNotBonusSplit() {
        YearlySubmission submission = new YearlySubmission(
                2025, "21809", "Vorboss", "Vorboss", "00000001", "250 to 499",
                81.4, 38.0, 12.0, 10.0, 80.0, 20.0,
                60.0, 40.0, 55.0, 45.0, 52.0, 48.0, 50.0, 50.0,
                false
        );

        CompanyStoreSnapshot snapshot = compiler.compile(Map.of(2025, List.of(submission)));
        CompanyYearSummary summary = snapshot.companies().get(0).yearlySummaries().get(0);

        assertThat(summary.femaleOverallPercent()).isEqualTo(45.75);
        assertThat(summary.maleOverallPercent()).isEqualTo(54.25);
        assertThat(summary.femaleOverallPercent() + summary.maleOverallPercent()).isEqualTo(100.0);
    }

    @Test
    void yearlySummaryInfersMissingQuartilePairValue() {
        YearlySubmission submission = new YearlySubmission(
                2025, "EMP-2", "Example 2", "Example 2", "00000002", "250 to 499",
                90.0, 20.0, null, null, null, null,
                55.0, null, null, 45.0, null, null, null, null,
                false
        );

        CompanyStoreSnapshot snapshot = compiler.compile(Map.of(2025, List.of(submission)));
        CompanyYearSummary summary = snapshot.companies().get(0).yearlySummaries().get(0);

        assertThat(summary.femaleOverallPercent()).isEqualTo(45.0);
        assertThat(summary.maleOverallPercent()).isEqualTo(55.0);
    }
}
