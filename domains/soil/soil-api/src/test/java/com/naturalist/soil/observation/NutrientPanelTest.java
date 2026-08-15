package com.naturalist.soil.observation;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The panel's tolerance of a lab that does not report every row. The three grouping read models
 * name the FGL <em>tomato</em> panel's seventeen rows; a panel run for another crop may be
 * shorter, and a short panel is a valid panel — not a violated invariant.
 */
class NutrientPanelTest {

    private static final Observer observer = Observer.forClass(NutrientPanelTest.class);

    @Test
    void panelMissingEveryNutrientHasNoViolations() {
        var mo = observer.forMethod("panelMissingEveryNutrientHasNoViolations");

        InvariantObservation result = mo.observable(emptyPanel(), "panel");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void panelReportingSomeNutrientsHasNoViolations() {
        var mo = observer.forMethod("panelReportingSomeNutrientsHasNoViolations");
        var panel = new NutrientPanel(
                new PrimaryNutrients(
                        Optional.of(reading(Nutrients.NITRATE_N, "1.36")),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()),
                emptyPanel().secondary(),
                emptyPanel().micro());

        InvariantObservation result = mo.observable(panel, "panel");

        assertThat(result.violations()).isEmpty();
        assertThat(panel.primary().nitrateN()).isPresent();
        assertThat(panel.primary().phosphorusP2O5()).isEmpty();
    }

    /**
     * A reported zero and an unreported nutrient are different facts. The panel keeps them
     * distinguishable; no consumer may collapse one into the other.
     */
    @Test
    void reportedZeroIsDistinctFromUnreported() {
        var panel = new NutrientPanel(
                new PrimaryNutrients(
                        Optional.of(reading(Nutrients.NITRATE_N, "0.00")),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()),
                emptyPanel().secondary(),
                emptyPanel().micro());

        assertThat(panel.primary().nitrateN()).isPresent();
        assertThat(panel.primary().nitrateN().orElseThrow().value()).isEqualByComparingTo("0.00");
        assertThat(panel.primary().phosphorusP2O5()).isEmpty();
    }

    /** A null slot is a caller slip, not a third state — it normalises to absent. */
    @Test
    void nullSlotNormalisesToEmpty() {
        var mo = observer.forMethod("nullSlotNormalisesToEmpty");
        var primary = new PrimaryNutrients(null, null, null, null);

        InvariantObservation result = mo.observable(
                new NutrientPanel(primary, emptyPanel().secondary(), emptyPanel().micro()), "panel");

        assertThat(result.violations()).isEmpty();
        assertThat(primary.nitrateN()).isEmpty();
    }

    private static NutrientPanel emptyPanel() {
        return new NutrientPanel(
                new PrimaryNutrients(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                new SecondaryNutrients(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()),
                new MicroNutrients(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()));
    }

    private static NutrientReading reading(NutrientName nutrientName, String value) {
        return new NutrientReading(
                NutrientReadingId.create(),
                nutrientName,
                LabAnalysisId.create(),
                new BigDecimal(value),
                MeasurementUnit.LBS_PER_1000_SQFT);
    }
}
