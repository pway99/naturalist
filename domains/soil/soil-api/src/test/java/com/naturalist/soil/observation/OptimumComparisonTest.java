package com.naturalist.soil.observation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OptimumComparisonTest {

    private static final OptimumRange NITRATE_N_BOX1 =
            new OptimumRange.Closed(new BigDecimal("5.3"), new BigDecimal("7.2"));

    @Test
    void valueInsideAClosedRangeIsWithin() {
        assertThat(OptimumComparison.of(new BigDecimal("6.0"), NITRATE_N_BOX1).verdict())
                .isEqualTo(OptimumComparison.Verdict.WITHIN);
    }

    /** Box 1's actual nitrate-N: 1.36 against 5.3 – 7.2. */
    @Test
    void valueUnderAClosedRangeIsBelow() {
        assertThat(OptimumComparison.of(new BigDecimal("1.36"), NITRATE_N_BOX1).verdict())
                .isEqualTo(OptimumComparison.Verdict.BELOW);
    }

    /** Bounds are inclusive, as the report reads them. */
    @Test
    void valuesExactlyOnEitherBoundAreWithin() {
        assertThat(OptimumComparison.of(new BigDecimal("5.3"), NITRATE_N_BOX1).verdict())
                .isEqualTo(OptimumComparison.Verdict.WITHIN);
        assertThat(OptimumComparison.of(new BigDecimal("7.2"), NITRATE_N_BOX1).verdict())
                .isEqualTo(OptimumComparison.Verdict.WITHIN);
    }

    /** Soluble sodium, {@code < 19}: no floor exists, so nothing can sit below it. */
    @Test
    void anUpperBoundedRangeHasNoBelow() {
        OptimumRange ceiling = new OptimumRange.UpperBounded(new BigDecimal("19"));

        assertThat(OptimumComparison.of(BigDecimal.ZERO, ceiling).verdict())
                .isEqualTo(OptimumComparison.Verdict.WITHIN);
        assertThat(OptimumComparison.of(new BigDecimal("20"), ceiling).verdict())
                .isEqualTo(OptimumComparison.Verdict.ABOVE);
    }

    @Test
    void aNotApplicableRangeYieldsNoComparison() {
        OptimumComparison result =
                OptimumComparison.of(new BigDecimal("1"), new OptimumRange.NotApplicable());

        assertThat(result.verdict()).isEqualTo(OptimumComparison.Verdict.NOT_COMPARABLE);
        assertThat(result.isConclusive()).isFalse();
    }

    @Test
    void anAbsentReadingOrAbsentOptimumYieldsNoComparison() {
        assertThat(OptimumComparison.of(Optional.empty(), Optional.empty()).isConclusive()).isFalse();
    }

    /**
     * Every comparison this type produces is attributed to us, never to the lab. The distinction
     * is not decorative — see {@code SoilProfileViewTest}, where our arithmetic disagrees with
     * FGL's own bar.
     */
    @Test
    void everyComparisonIsAttributedToUsRatherThanTheLab() {
        assertThat(OptimumComparison.of(new BigDecimal("6.0"), NITRATE_N_BOX1).source())
                .isEqualTo(AssessmentSource.DERIVED_FROM_PRINTED_RANGE);
        assertThat(OptimumComparison.notComparable().source())
                .isEqualTo(AssessmentSource.DERIVED_FROM_PRINTED_RANGE);
    }
}
