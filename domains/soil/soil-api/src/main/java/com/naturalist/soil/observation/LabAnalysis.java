package com.naturalist.soil.observation;

import com.naturalist.ddd.FactEntity;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * An immutable record of a soil laboratory analysis performed on a soil sample from
 * a specific zone or sub-zone at Oak Vista.
 * <p>
 * {@code LabAnalysis} is a child entity within the {@link com.naturalist.soil.SoilProfile}
 * aggregate. It is never modified — analysis history is append-only. Each {@code LabAnalysis}
 * records who performed the analysis, what their sample reference was, when the sample
 * was collected, and the full {@link NutrientPanel} reported.
 * <p>
 * <b>Current lab partner:</b> Fruit Growers Laboratory (FGL), Fresno CA.
 * FGL sample IDs follow the format {@code CH XXXXXXX-NNN} (e.g. {@code CH 2671853-001}).
 * The lab turnaround for standard reports is approximately three weeks from sample submission.
 * <p>
 * <b>Oak Vista analyses on record:</b>
 * <ul>
 *   <li>Garden Box 1, sampled March 3, 2026 (FGL CH 2671853-001).
 *       Reveals: N VERY_LOW, P VERY_HIGH, K-Sol VERY_LOW, Ca-Sol VERY_LOW, Sulfate VERY_LOW,
 *       Boron VERY_LOW, limestone 1.7%.</li>
 *   <li>Backyard garden, sampled March 3, 2026 (FGL CH 2671853-002).
 *       Similar deficiency pattern; limestone 2.9% (higher CaCO₃ reserve for Thiobacillus
 *       conversion).</li>
 * </ul>
 */
public record LabAnalysis(
        LabAnalysisName name,
        LocalDate sampleDate,
        String labId,
        String labSampleId,
        @Nullable String notes,
        NutrientPanel nutrients
) implements FactEntity<LabAnalysisName> {

    // ── Domain queries ─────────────────────────────────────────────────────────

    /**
     * Whether this analysis indicates a BER (blossom end rot) risk for Solanaceae
     * crops at this soil location.
     * <p>
     * BER at Oak Vista is driven by the combination of: (1) very low Ca-Sol preventing
     * adequate calcium uptake during rapid fruit development, and (2) boron deficiency
     * impairing calcium transport from root to fruit. Either condition alone is a risk;
     * both together make BER a near-certainty without corrective inputs.
     *
     * @return {@code true} if Ca-Sol is VERY_LOW or LOW, or if boron is VERY_LOW or LOW
     */
    public boolean indicatesBerRisk() {
        NutrientStatus caSolStatus = nutrients.calciumSoluble().status();
        NutrientStatus boronStatus = nutrients.boron().status();
        return caSolStatus == NutrientStatus.VERY_LOW || caSolStatus == NutrientStatus.LOW
                || boronStatus == NutrientStatus.VERY_LOW || boronStatus == NutrientStatus.LOW;
    }

    /**
     * Whether this analysis shows a Thiobacillus-amenable limestone accumulation.
     * <p>
     * Thiobacillus bacteria oxidise elemental sulfur to sulfuric acid above 77°F,
     * which reacts with CaCO₃ limestone to form CaSO₄ (gypsum) in situ. This pathway
     * converts insoluble limestone calcium to plant-available Ca-Sol and simultaneously
     * provides sulfate. The reaction is agronomically beneficial at Oak Vista's 7.2 pH
     * because it raises Ca-Sol and SO₄ without lowering pH below the target range.
     * <p>
     * A limestone content above 0.5% indicates sufficient CaCO₃ reserve for this
     * reaction to be agronomically meaningful over a growing season.
     *
     * @return {@code true} if limestone percentage exceeds 0.5% (the FGL "no problem" threshold)
     */
    public boolean hasThiobacillusAmenableLimestone() {
        return nutrients.limestonePct().isAbove(new BigDecimal("0.5"));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .factName(name, "name")
                .notNull(this, LabAnalysis::sampleDate, "sampleDate")
                .notNull(this, LabAnalysis::labId, "labId")
                .notNull(this, LabAnalysis::labSampleId, "labSampleId")
                .notNull(this, LabAnalysis::nutrients, "nutrients");
    }
}
