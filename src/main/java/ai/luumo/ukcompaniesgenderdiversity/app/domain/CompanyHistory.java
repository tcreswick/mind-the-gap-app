package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.util.List;
import java.util.Set;

public record CompanyHistory(
        String employerId,
        String displayName,
        String companyNumber,
        String employerSize,
        Set<String> nameAliases,
        List<Integer> submittedYears,
        List<CompanyYearSummary> yearlySummaries
) {
}
