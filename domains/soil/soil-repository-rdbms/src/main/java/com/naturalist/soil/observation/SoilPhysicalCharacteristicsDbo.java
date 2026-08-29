package com.naturalist.soil.observation;

import com.naturalist.measurements.ElectricalConductivity;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.soil.SoilPH;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link SoilPhysicalCharacteristics} — a surrogate-UUID {@code Entity},
 * one row per analysis. Both uuid references travel as text with a {@code ::uuid} cast; the
 * {@code lab_analysis_id} soft reference is a plain column (no FK). Every measurement value type
 * flattens onto an unconstrained {@code NUMERIC} column via {@code .value()} and rebuilds via its
 * {@code of(BigDecimal)} factory, round-tripping at its exact scale. The owned
 * {@link CationBaseSaturation} value object flattens onto the six {@code cbs_*} columns plus the
 * boolean detection-limit flag.
 * <p>
 * The single-column {@code lab_analysis_id} UNIQUE (the 1:1 grain) is enforced by DDL only — it is
 * not declared on {@code @DboSchema}.
 */
@DboSchema(table = "soil_physical_characteristics", primaryKey = "id", entity = SoilPhysicalCharacteristics.class)
final class SoilPhysicalCharacteristicsDbo implements Dbo {
    String id;
    String labAnalysisId;
    BigDecimal cecMeqPer100g;
    BigDecimal ph;
    BigDecimal ecDsPerMeter;
    BigDecimal sar;
    BigDecimal limestonePct;
    BigDecimal saturationPct;
    // CationBaseSaturation
    BigDecimal cbsCalciumPct;
    BigDecimal cbsMagnesiumPct;
    BigDecimal cbsPotassiumPct;
    BigDecimal cbsSodiumPct;
    BigDecimal cbsHydrogenPct;
    boolean cbsHydrogenBelowDetectionLimit;

    static SoilPhysicalCharacteristicsDbo from(SoilPhysicalCharacteristics s) {
        SoilPhysicalCharacteristicsDbo d = new SoilPhysicalCharacteristicsDbo();
        d.id = s.id().value().toString();
        d.labAnalysisId = s.labAnalysisId().value().toString();
        d.cecMeqPer100g = s.cecMeqPer100g().value();
        d.ph = s.pH().value();
        d.ecDsPerMeter = s.ecDsPerMeter().value();
        d.sar = s.sar().value();
        d.limestonePct = s.limestonePct().value();
        d.saturationPct = s.saturationPct().value();
        CationBaseSaturation cbs = s.cationBaseSaturation();
        d.cbsCalciumPct = cbs.calciumPct();
        d.cbsMagnesiumPct = cbs.magnesiumPct();
        d.cbsPotassiumPct = cbs.potassiumPct();
        d.cbsSodiumPct = cbs.sodiumPct();
        d.cbsHydrogenPct = cbs.hydrogenPct();
        d.cbsHydrogenBelowDetectionLimit = cbs.hydrogenBelowDetectionLimit();
        Observer.forClass(SoilPhysicalCharacteristicsDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    SoilPhysicalCharacteristics toEntity() {
        return new SoilPhysicalCharacteristics(
                SoilPhysicalCharacteristicsId.of(UUID.fromString(id)),
                LabAnalysisId.of(UUID.fromString(labAnalysisId)),
                CecMeqPer100g.of(cecMeqPer100g),
                SoilPH.of(ph),
                ElectricalConductivity.of(ecDsPerMeter),
                SodiumAdsorptionRatio.of(sar),
                LimestonePct.of(limestonePct),
                SaturationPct.of(saturationPct),
                new CationBaseSaturation(
                        cbsCalciumPct, cbsMagnesiumPct, cbsPotassiumPct,
                        cbsSodiumPct, cbsHydrogenPct, cbsHydrogenBelowDetectionLimit));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notBlank(labAnalysisId, "labAnalysisId")
                .notNull(cecMeqPer100g, "cecMeqPer100g")
                .notNull(ph, "ph")
                .notNull(ecDsPerMeter, "ecDsPerMeter")
                .notNull(sar, "sar")
                .notNull(limestonePct, "limestonePct")
                .notNull(saturationPct, "saturationPct")
                .notNull(cbsCalciumPct, "cbsCalciumPct")
                .notNull(cbsMagnesiumPct, "cbsMagnesiumPct")
                .notNull(cbsPotassiumPct, "cbsPotassiumPct")
                .notNull(cbsSodiumPct, "cbsSodiumPct")
                .notNull(cbsHydrogenPct, "cbsHydrogenPct");
    }
}
