package com.naturalist.soil.observation;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * One row of what a lab recommended doing about a soil analysis — a Fertilization Recommendations
 * row, or a lime/gypsum requirement. Append-only report fact, sibling to the analysis, exactly as
 * {@link ReportedOptimum} is.
 * <p>
 * <b>This is what the lab said, not what we intend to do.</b> The distinction matters more here
 * than anywhere else in the domain, because a recommendation looks like an instruction. It is
 * evidence: FGL advised 11.2 lbs/1000 ft² of K₂O on Box 1 in March 2026. Whether that was applied,
 * when, and at what rate is an {@code AmendmentEvent} — a different fact with a different author.
 * Nothing here should ever be read as a record of an application.
 * <p>
 * <b>No optimum range.</b> The requirements block prints {@code ---} in its optimum column, and
 * the fertilisation table has no optimum column at all. A recommendation is already an
 * interpretation the lab performed; layering a target on top of it would be interpreting an
 * interpretation.
 * <p>
 * Identity is the surrogate {@link ReportedRecommendationId}; the logical key
 * {@code (inputName, labAnalysisId)} is enforced as a unique constraint by the data source.
 */
public record ReportedRecommendation(
        ReportedRecommendationId id,
        RecommendedInputName inputName,
        LabAnalysisId labAnalysisId,
        RecommendedAmount amount,
        MeasurementUnit unit,
        @Nullable ApplicationRoute route
) implements Entity<ReportedRecommendationId> {

    /**
     * How the lab says to deliver the input — the "via" column. Null on the requirements block,
     * which prints no route: a lime requirement is a quantity the soil needs, not an application
     * instruction. Every March 2026 fertilisation row reads {@code Soil}.
     */
    public enum ApplicationRoute {
        SOIL,
        FOLIAR
    }

    /** Whether the lab recommended applying anything at all for this input. */
    public boolean recommendsApplication() {
        return !(amount instanceof RecommendedAmount.None);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(inputName, "inputName")
                .entityId(labAnalysisId, "labAnalysisId")
                .valueObject(amount, "amount")
                .notNull(unit, "unit");
    }
}
