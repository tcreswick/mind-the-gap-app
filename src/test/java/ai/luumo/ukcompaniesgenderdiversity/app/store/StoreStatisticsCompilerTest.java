package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.StoreStatisticsSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StoreStatisticsCompilerTest {
    private final StoreStatisticsCompiler compiler = new StoreStatisticsCompiler();

    @Test
    void compileBuildsRollingThirtyDayDailyCounts() {
        Instant now = Instant.parse("2026-04-03T12:00:00Z");

        CompanyHistory companyA = companyWithSubmissions(
                "EMP-1",
                Instant.parse("2026-04-03T09:00:00Z"),
                Instant.parse("2026-04-02T10:00:00Z"),
                Instant.parse("2026-03-05T23:59:59Z")
        );
        CompanyHistory companyB = companyWithSubmissions(
                "EMP-2",
                Instant.parse("2026-04-02T15:00:00Z"),
                Instant.parse("2026-03-04T23:59:59Z"),
                null
        );

        StoreStatisticsSnapshot snapshot = compiler.compile(List.of(companyA, companyB), now);

        assertThat(snapshot.submissionsPerDayLast30Days()).hasSize(30);
        assertThat(snapshot.submissionsPerDayLast30Days().getFirst().day())
                .isEqualTo(LocalDate.parse("2026-03-05"));
        assertThat(snapshot.submissionsPerDayLast30Days().getLast().day())
                .isEqualTo(LocalDate.parse("2026-04-03"));

        assertThat(countFor(snapshot, LocalDate.parse("2026-03-05"))).isEqualTo(1);
        assertThat(countFor(snapshot, LocalDate.parse("2026-04-02"))).isEqualTo(2);
        assertThat(countFor(snapshot, LocalDate.parse("2026-04-03"))).isEqualTo(1);
    }

    private CompanyHistory companyWithSubmissions(String employerId, Instant... submittedAtValues) {
        List<CompanyYearSummary> summaries = java.util.Arrays.stream(submittedAtValues)
                .map(submittedAt -> new CompanyYearSummary(
                        2025,
                        "Example Co",
                        45.0,
                        55.0,
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
                        submittedAt
                ))
                .toList();
        return new CompanyHistory(
                employerId,
                "Example Co",
                null,
                null,
                Set.of(),
                List.of(2025),
                summaries
        );
    }

    private int countFor(StoreStatisticsSnapshot snapshot, LocalDate day) {
        return snapshot.submissionsPerDayLast30Days().stream()
                .filter(entry -> entry.day().equals(day))
                .findFirst()
                .orElseThrow()
                .submissionCount();
    }
}
