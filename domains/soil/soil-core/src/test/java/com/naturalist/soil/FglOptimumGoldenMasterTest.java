package com.naturalist.soil;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.soil.observation.LabAnalysis;
import com.naturalist.soil.observation.NutrientName;
import com.naturalist.soil.observation.Nutrients;
import com.naturalist.soil.observation.OptimumRange;
import com.naturalist.soil.observation.ReportedOptimum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>The golden master.</b> FGL's printed "optimum range" for the four exchangeable cations is not
 * an opinion the lab formed about each sample — it is arithmetic. The lab takes crop-invariant
 * base-saturation targets (Ca 60–80%, Mg 10–20%, K 1.0–6.0%, Na 0.0–5.0% of CEC, identical on both
 * March 2026 reports) and projects them through <em>that sample's own CEC</em>.
 * <p>
 * This test reproduces all sixteen printed bounds across both analyses from those four percentage
 * pairs and each sample's CEC, and asserts the result equals what the lab actually printed, to the
 * two significant figures the report carries.
 * <p>
 * <b>Why it is worth having.</b> It is the honest substitute for the not-yet-existing agronomy
 * module: it pins down what {@code FglReplicaStrategy} will have to reproduce, using data no
 * strategy of ours produced. It also answers the crop-invariance question directly — for these
 * four rows the "tomato optimum" is not about tomatoes at all, so the lettuce panel arriving in
 * August must show the same percentages against a different CEC. If it does not, this test is
 * where that discovery happens.
 * <p>
 * <b>If this test fails</b> after new data lands, the interesting possibilities are: the lab
 * changed its base-saturation targets, the lab changed its soil-mass basis, or a transcription in
 * {@code reported-optimum.json} is wrong. Fix the transcription; do not relax the assertion to
 * make a real change in FGL's method disappear.
 */
class FglOptimumGoldenMasterTest {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    private final SoilProfileQuery query = SoilsTestContextInternal.create(db).soilProfileQuery();

    /**
     * Converts meq/100g of a cation to lbs/1000 ft². Empirically 0.91872, which is a soil mass of
     * ~91,900 lbs per 1000 ft² — a 12-inch profile at roughly 1.47 g/cm³. FGL does not print the
     * basis it uses; this is the constant that reproduces every bound on both reports.
     */
    private static final BigDecimal SOIL_MASS_FACTOR = new BigDecimal("0.91872");

    /** The report prints two significant figures ("120", "19", "0.060"). */
    private static final MathContext REPORT_PRECISION = new MathContext(2, RoundingMode.HALF_UP);

    /**
     * A cation's exchangeable row, its equivalent weight in mg/meq, and the crop-invariant base
     * saturation window the report prints for it. Potassium is reported as K₂O, so its equivalent
     * weight is that of K₂O (94.2 / 2), not of elemental potassium.
     */
    private record CationTarget(NutrientName exchangeableRow, String equivalentWeight,
                                String minPercentOfCec, String maxPercentOfCec) {
    }

    private static final List<CationTarget> TARGETS = List.of(
            new CationTarget(Nutrients.CALCIUM_EXCHANGEABLE, "20.04", "60", "80"),
            new CationTarget(Nutrients.MAGNESIUM_EXCHANGEABLE, "12.155", "10", "20"),
            new CationTarget(Nutrients.POTASSIUM_EXCHANGEABLE, "47.1", "1.0", "6.0"),
            new CationTarget(Nutrients.SODIUM_EXCHANGEABLE, "22.99", "0.0", "5.0"));

    @Test
    void box1_exchangeableOptimaAreBaseSaturationTargetsProjectedThroughItsCec() {
        assertProjectionReproducesReport(TestSoilIdentifiers.SoilProfiles.Box1.name);
    }

    @Test
    void backyard_exchangeableOptimaAreBaseSaturationTargetsProjectedThroughItsCec() {
        assertProjectionReproducesReport(TestSoilIdentifiers.SoilProfiles.Backyard.name);
    }

    /**
     * The same four percentage pairs reproduce both reports despite their different CECs (44.9 vs
     * 34.2 meq/100g). That is the crop-invariance claim stated as an assertion rather than a note:
     * the per-sample numbers differ, the targets behind them do not.
     */
    @Test
    void bothAnalysesShareTheSameTargetsDespiteDifferentCecAndPrintedRanges() {
        BigDecimal box1Cec = cecOf(TestSoilIdentifiers.SoilProfiles.Box1.name);
        BigDecimal backyardCec = cecOf(TestSoilIdentifiers.SoilProfiles.Backyard.name);

        assertThat(box1Cec).isNotEqualByComparingTo(backyardCec);
        assertThat(closedRange(TestSoilIdentifiers.SoilProfiles.Box1.name, Nutrients.CALCIUM_EXCHANGEABLE).min())
                .isNotEqualByComparingTo(
                        closedRange(TestSoilIdentifiers.SoilProfiles.Backyard.name, Nutrients.CALCIUM_EXCHANGEABLE).min());
        // ...yet both are 60% of their own CEC.
        assertProjectionReproducesReport(TestSoilIdentifiers.SoilProfiles.Box1.name);
        assertProjectionReproducesReport(TestSoilIdentifiers.SoilProfiles.Backyard.name);
    }

    private void assertProjectionReproducesReport(SoilProfileName profileName) {
        BigDecimal cec = cecOf(profileName);

        for (CationTarget target : TARGETS) {
            OptimumRange.Closed printed = closedRange(profileName, target.exchangeableRow());

            assertThat(project(target.minPercentOfCec(), cec, target.equivalentWeight()))
                    .as("%s %s: %s%% of CEC %s", profileName.value(), target.exchangeableRow().value(),
                            target.minPercentOfCec(), cec)
                    .isEqualByComparingTo(printed.min());
            assertThat(project(target.maxPercentOfCec(), cec, target.equivalentWeight()))
                    .as("%s %s: %s%% of CEC %s", profileName.value(), target.exchangeableRow().value(),
                            target.maxPercentOfCec(), cec)
                    .isEqualByComparingTo(printed.max());
        }
    }

    /** percentOfCec / 100 × CEC × equivalentWeight × soil-mass factor, at report precision. */
    private static BigDecimal project(String percentOfCec, BigDecimal cec, String equivalentWeight) {
        BigDecimal projected = new BigDecimal(percentOfCec)
                .movePointLeft(2)
                .multiply(cec)
                .multiply(new BigDecimal(equivalentWeight))
                .multiply(SOIL_MASS_FACTOR);
        return projected.signum() == 0 ? BigDecimal.ZERO : projected.round(REPORT_PRECISION);
    }

    private BigDecimal cecOf(SoilProfileName profileName) {
        return latestAnalysis(profileName).physicalCharacteristics().orElseThrow()
                .cecMeqPer100g().value();
    }

    private OptimumRange.Closed closedRange(SoilProfileName profileName, NutrientName nutrientName) {
        ReportedOptimum optimum = latestAnalysis(profileName).reportedOptima()
                .forNutrient(nutrientName)
                .orElseThrow();
        assertThat(optimum.range())
                .as("%s %s is a closed range on the report", profileName.value(), nutrientName.value())
                .isInstanceOf(OptimumRange.Closed.class);
        return (OptimumRange.Closed) optimum.range();
    }

    private LabAnalysis latestAnalysis(SoilProfileName profileName) {
        return query.getBySoilProfileName(profileName).orElseThrow().latestLabAnalysis().orElseThrow();
    }
}
