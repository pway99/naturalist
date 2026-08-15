package com.naturalist.soil.observation;

import com.naturalist.ddd.Entity;
import com.naturalist.measurements.DepthInches;
import com.naturalist.observability.Constraints;
import com.naturalist.soil.CropName;
import com.naturalist.soil.SoilProfileName;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * The header of a soil laboratory analysis — its identity, the soil profile it sampled, the crop
 * context it was interpreted for, and the lab/sample provenance. The chemistry itself is stored
 * separately as the analysis's {@link NutrientReading} entities and its
 * {@link SoilPhysicalCharacteristics} row (joined by {@code labAnalysisId}); the assembled whole is
 * a {@link LabAnalysis}.
 * <p>
 * {@code crop} is a {@link CropName} soft-reference — the interpretation context (which crop's
 * optimum ranges apply); the rich crop-planting entity is a separate effort. Immutable,
 * append-only — a lab report is a historical fact. Current lab: Fruit Growers Laboratory (FGL),
 * Chico CA; sample IDs {@code CH XXXXXXX-NNN}.
 * <p>
 * <b>Sampling provenance.</b> {@code sampleDepth} is the depth the lab reports, and
 * {@code samplingProtocol} is how the material was drawn. Both are nullable, and both are null on
 * the March 2026 analyses — the reports print {@code Depth: N/A} and no protocol was recorded.
 * That absence is the historical truth and is load-bearing: a nutrient value means something
 * different at four inches than at twelve, so an analysis that cannot say which is an analysis
 * that must not be silently compared against one that can. Do not back-fill either field with a
 * plausible value.
 */
public record LabAnalysisInfo(
        LabAnalysisId id,
        SoilProfileName soilProfileName,
        CropName crop,
        LocalDate sampleDate,
        String labId,
        String labSampleId,
        @Nullable DepthInches sampleDepth,
        @Nullable SamplingProtocol samplingProtocol,
        @Nullable String notes
) implements Entity<LabAnalysisId> {

    /** Whether this analysis records how and how deep it was sampled. */
    public boolean hasSamplingProvenance() {
        return sampleDepth != null && samplingProtocol != null;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(soilProfileName, "soilProfileName")
                .entityName(crop, "crop")
                .notNull(sampleDate, "sampleDate")
                .notNull(labId, "labId")
                .notNull(labSampleId, "labSampleId")
                .whenNotNull(sampleDepth, c -> c.namedValue(sampleDepth, "sampleDepth"))
                .valueObjectOrNull(samplingProtocol, "samplingProtocol");
    }
}
