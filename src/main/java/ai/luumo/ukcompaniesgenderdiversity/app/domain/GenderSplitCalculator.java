package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;

public final class GenderSplitCalculator {
    private GenderSplitCalculator() {
    }

    public static Double average(Double... values) {
        DoubleSummaryStatistics stats = Arrays.stream(values)
                .filter(v -> v != null && !Double.isNaN(v))
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        return stats.getCount() == 0 ? null : stats.getAverage();
    }
}
