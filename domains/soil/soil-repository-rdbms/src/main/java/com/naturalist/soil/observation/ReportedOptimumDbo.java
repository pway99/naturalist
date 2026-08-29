package com.naturalist.soil.observation;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link ReportedOptimum} — a surrogate-UUID {@code Entity}. The sealed
 * {@link OptimumRange} value object is flattened adapter-side onto a {@code range_shape} discriminator
 * plus nullable {@code range_min}/{@code range_max} columns (the type carries no {@code kind()}
 * accessor, so the DBO switches over the permits). Both uuid references travel as text with a
 * {@code ::uuid} cast; the {@code lab_analysis_id} soft reference is a plain column (no FK).
 * <p>
 * The logical {@code (nutrient_name, lab_analysis_id)} composite UNIQUE is DDL-only — not declared
 * on {@code @DboSchema}.
 */
@DboSchema(table = "reported_optimum", primaryKey = "id", entity = ReportedOptimum.class)
final class ReportedOptimumDbo implements Dbo {
    String id;
    String nutrientName;
    String labAnalysisId;
    String rangeShape;
    BigDecimal rangeMin;   // set for CLOSED, LOWER_BOUNDED
    BigDecimal rangeMax;   // set for CLOSED, UPPER_BOUNDED
    String unit;

    static ReportedOptimumDbo from(ReportedOptimum o) {
        ReportedOptimumDbo d = new ReportedOptimumDbo();
        d.id = o.id().value().toString();
        d.nutrientName = o.nutrientName().value();
        d.labAnalysisId = o.labAnalysisId().value().toString();
        switch (o.range()) {
            case OptimumRange.Closed c -> {
                d.rangeShape = "CLOSED";
                d.rangeMin = c.min();
                d.rangeMax = c.max();
            }
            case OptimumRange.UpperBounded u -> {
                d.rangeShape = "UPPER_BOUNDED";
                d.rangeMax = u.max();
            }
            case OptimumRange.LowerBounded l -> {
                d.rangeShape = "LOWER_BOUNDED";
                d.rangeMin = l.min();
            }
            case OptimumRange.NotApplicable n -> d.rangeShape = "NOT_APPLICABLE";
        }
        d.unit = o.unit().name();
        Observer.forClass(ReportedOptimumDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    ReportedOptimum toEntity() {
        OptimumRange range = switch (rangeShape) {
            case "CLOSED" -> new OptimumRange.Closed(rangeMin, rangeMax);
            case "UPPER_BOUNDED" -> new OptimumRange.UpperBounded(rangeMax);
            case "LOWER_BOUNDED" -> new OptimumRange.LowerBounded(rangeMin);
            case "NOT_APPLICABLE" -> new OptimumRange.NotApplicable();
            default -> throw new IllegalStateException("unknown range shape: " + rangeShape);
        };
        return new ReportedOptimum(
                ReportedOptimumId.of(UUID.fromString(id)),
                NutrientName.of(nutrientName),
                LabAnalysisId.of(UUID.fromString(labAnalysisId)),
                range,
                MeasurementUnit.valueOf(unit));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(nutrientName, "nutrientName").kebabFormat(nutrientName, "nutrientName")
                .maxLength(nutrientName, 48, "nutrientName")
                .notBlank(labAnalysisId, "labAnalysisId")
                .notBlank(rangeShape, "rangeShape").maxLength(rangeShape, 16, "rangeShape")
                .notBlank(unit, "unit").maxLength(unit, 24, "unit");
    }
}
