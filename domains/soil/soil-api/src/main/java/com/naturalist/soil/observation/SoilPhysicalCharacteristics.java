package com.naturalist.soil.observation;

import com.naturalist.ddd.Entity;
import com.naturalist.measurements.ElectricalConductivity;
import com.naturalist.observability.Constraints;
import com.naturalist.soil.SoilPH;

import java.util.function.Consumer;

/**
 * The physical and derived soil properties of a single analysis — a family distinct from the
 * nutrient readings, kept richly typed. Where nutrients are many, homogeneous, and lab-variable
 * (hence the open {@link NutrientReading} grain with an explicit unit), the physical characteristics
 * are a small standard set with heterogeneous typed values, so they keep their measurement types
 * (pH bounded 0–14, EC in dS/m, CEC in meq/100g, …) — the unit is intrinsic to the type.
 * <p>
 * Measured fact only — targets and thresholds (e.g. limestone amenability, pH bands) are
 * interpretation and belong to the CropProfile effort. One row per {@link LabAnalysisInfo}; identity is
 * the surrogate {@link SoilPhysicalCharacteristicsId} with {@code labAnalysisId} enforced unique.
 */
public record SoilPhysicalCharacteristics(
        SoilPhysicalCharacteristicsId id,
        LabAnalysisId labAnalysisId,
        CecMeqPer100g cecMeqPer100g,
        SoilPH pH,
        ElectricalConductivity ecDsPerMeter,
        SodiumAdsorptionRatio sar,
        LimestonePct limestonePct,
        SaturationPct saturationPct,
        CationBaseSaturation cationBaseSaturation
) implements Entity<SoilPhysicalCharacteristicsId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityId(labAnalysisId, "labAnalysisId")
                .namedValue(cecMeqPer100g, "cecMeqPer100g")
                .namedValue(pH, "pH")
                .namedValue(ecDsPerMeter, "ecDsPerMeter")
                .namedValue(sar, "sar")
                .namedValue(limestonePct, "limestonePct")
                .namedValue(saturationPct, "saturationPct")
                .valueObject(cationBaseSaturation, "cationBaseSaturation");
    }
}
