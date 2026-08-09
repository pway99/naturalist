package com.naturalist.soil.observation;

import com.naturalist.ddd.Entity;
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
 * a {@code LabAnalysisView}.
 * <p>
 * {@code crop} is a {@link CropName} soft-reference — the interpretation context (which crop's
 * optimum ranges apply); the rich crop-planting entity is a separate effort. Immutable,
 * append-only — a lab report is a historical fact. Current lab: Fruit Growers Laboratory (FGL),
 * Chico CA; sample IDs {@code CH XXXXXXX-NNN}.
 */
public record LabAnalysisInfo(
        LabAnalysisId id,
        SoilProfileName soilProfileName,
        CropName crop,
        LocalDate sampleDate,
        String labId,
        String labSampleId,
        @Nullable String notes
) implements Entity<LabAnalysisId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(soilProfileName, "soilProfileName")
                .entityName(crop, "crop")
                .notNull(sampleDate, "sampleDate")
                .notNull(labId, "labId")
                .notNull(labSampleId, "labSampleId");
    }
}
