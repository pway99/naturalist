package com.naturalist.soil.observation;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link ReportedRecommendation} — a surrogate-UUID {@code Entity}. The
 * sealed {@link RecommendedAmount} value object is flattened adapter-side onto an {@code amount_kind}
 * discriminator plus a nullable {@code amount_value} column (the type carries no {@code kind()}
 * accessor, so the DBO switches over the permits). A {@code Quantity(0)} stores {@code amount_value = 0}
 * (non-null) while a {@code None} stores {@code amount_value = null} — the discriminator, not the
 * nullness, tells them apart, so the two never collapse. Both uuid references travel as text with a
 * {@code ::uuid} cast; the {@code lab_analysis_id} soft reference is a plain column (no FK).
 * <p>
 * The logical {@code (input_name, lab_analysis_id)} composite UNIQUE is DDL-only — not declared on
 * {@code @DboSchema}.
 */
@DboSchema(table = "reported_recommendation", primaryKey = "id", entity = ReportedRecommendation.class)
final class ReportedRecommendationDbo implements Dbo {
    String id;
    String inputName;
    String labAnalysisId;
    String amountKind;
    BigDecimal amountValue;   // Quantity.value (incl. 0) or BelowDetectionLimit.limit; null for NONE
    String unit;
    String route;             // nullable

    static ReportedRecommendationDbo from(ReportedRecommendation r) {
        ReportedRecommendationDbo d = new ReportedRecommendationDbo();
        d.id = r.id().value().toString();
        d.inputName = r.inputName().value();
        d.labAnalysisId = r.labAnalysisId().value().toString();
        switch (r.amount()) {
            case RecommendedAmount.Quantity q -> {
                d.amountKind = "QUANTITY";
                d.amountValue = q.value();
            }
            case RecommendedAmount.None n -> d.amountKind = "NONE";
            case RecommendedAmount.BelowDetectionLimit b -> {
                d.amountKind = "BELOW_DETECTION_LIMIT";
                d.amountValue = b.limit();
            }
        }
        d.unit = r.unit().name();
        d.route = r.route() == null ? null : r.route().name();
        Observer.forClass(ReportedRecommendationDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    ReportedRecommendation toEntity() {
        RecommendedAmount amount = switch (amountKind) {
            case "QUANTITY" -> new RecommendedAmount.Quantity(amountValue);
            case "NONE" -> new RecommendedAmount.None();
            case "BELOW_DETECTION_LIMIT" -> new RecommendedAmount.BelowDetectionLimit(amountValue);
            default -> throw new IllegalStateException("unknown amount kind: " + amountKind);
        };
        return new ReportedRecommendation(
                ReportedRecommendationId.of(UUID.fromString(id)),
                RecommendedInputName.of(inputName),
                LabAnalysisId.of(UUID.fromString(labAnalysisId)),
                amount,
                MeasurementUnit.valueOf(unit),
                route == null ? null : ReportedRecommendation.ApplicationRoute.valueOf(route));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(inputName, "inputName").kebabFormat(inputName, "inputName")
                .maxLength(inputName, 48, "inputName")
                .notBlank(labAnalysisId, "labAnalysisId")
                .notBlank(amountKind, "amountKind").maxLength(amountKind, 24, "amountKind")
                .notBlank(unit, "unit").maxLength(unit, 24, "unit")
                .maxLength(route, 8, "route");
    }
}
