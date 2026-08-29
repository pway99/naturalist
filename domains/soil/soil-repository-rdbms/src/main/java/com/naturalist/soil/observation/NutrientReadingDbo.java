package com.naturalist.soil.observation;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link NutrientReading} — a surrogate-UUID {@code Entity}, one reading
 * per nutrient per analysis. Both the {@code id} and the soft {@code labAnalysisId} reference travel
 * as text with a {@code ::uuid} cast (no FK, no JOIN — the analysis is a soft reference). The
 * {@code nutrientName} slug and the {@code MeasurementUnit} enum are plain columns; {@code value} is
 * an unconstrained {@code NUMERIC} that round-trips at its exact {@code BigDecimal} scale.
 * <p>
 * The logical {@code (nutrient_name, lab_analysis_id)} composite UNIQUE is enforced by DDL only —
 * it is not declared on {@code @DboSchema} (the validator checks single-column uniques).
 */
@DboSchema(table = "nutrient_reading", primaryKey = "id", entity = NutrientReading.class)
final class NutrientReadingDbo implements Dbo {
    String id;
    String nutrientName;
    String labAnalysisId;
    BigDecimal value;
    String unit;

    static NutrientReadingDbo from(NutrientReading r) {
        NutrientReadingDbo d = new NutrientReadingDbo();
        d.id = r.id().value().toString();
        d.nutrientName = r.nutrientName().value();
        d.labAnalysisId = r.labAnalysisId().value().toString();
        d.value = r.value();
        d.unit = r.unit().name();
        Observer.forClass(NutrientReadingDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    NutrientReading toEntity() {
        return new NutrientReading(
                NutrientReadingId.of(UUID.fromString(id)),
                NutrientName.of(nutrientName),
                LabAnalysisId.of(UUID.fromString(labAnalysisId)),
                value,
                MeasurementUnit.valueOf(unit));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(nutrientName, "nutrientName").kebabFormat(nutrientName, "nutrientName")
                .maxLength(nutrientName, 48, "nutrientName")
                .notBlank(labAnalysisId, "labAnalysisId")
                .notNull(value, "value")
                .notBlank(unit, "unit").maxLength(unit, 24, "unit");
    }
}
