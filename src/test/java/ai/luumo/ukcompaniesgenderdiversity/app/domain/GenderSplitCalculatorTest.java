package ai.luumo.ukcompaniesgenderdiversity.app.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenderSplitCalculatorTest {

    @Test
    void averageReturnsMeanOfPresentValues() {
        Double result = GenderSplitCalculator.average(50.0, null, 30.0, 20.0);
        assertThat(result).isEqualTo(33.333333333333336);
    }

    @Test
    void averageReturnsNullWhenNoValuesPresent() {
        Double result = GenderSplitCalculator.average(null, null);
        assertThat(result).isNull();
    }
}
