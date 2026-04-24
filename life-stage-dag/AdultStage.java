package com.naturalist.insects.lifestages;

import com.naturalist.fieldnotes.Description;
import com.naturalist.identifiers.LifeStageKind;
import com.naturalist.identifiers.LifeStageName;
import com.naturalist.identifiers.PlantName;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The adult stage. Flight timing lives in {@link StagePhenology}, not as a
 * separate field here.
 * <p>
 * {@code nectarSources} is typed as {@code List<PlantName>} — nectar is
 * produced only by flowering plants. Non-nectar adult feeding (sap, carrion,
 * blood, honeydew) is not modeled here; when needed, each becomes a separate
 * typed field (e.g. {@code sapSources}), not a generalization of nectar.
 * <p>
 * Adult predation (robber flies, mantids) produces no prey field — same reason
 * as {@link LarvaStage}: prey relationships live in a future ecology domain.
 */
public record AdultStage(
        LifeStageName name,
        StagePhenology phenology,
        StageHabitat habitat,
        @Nullable StageChemistryRole chemistryRole,
        Description description,
        @Nullable FeedingHabit feedingHabit,
        List<PlantName> nectarSources,
        @Nullable String ecologicalRole,
        @Nullable String lifespan
) implements LifeStage {

    @Override
    public LifeStageKind kind() {
        return LifeStageKind.ADULT;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .valueObject(phenology, "phenology")
                .valueObject(habitat, "habitat")
                .valueObjectOrNull(this, AdultStage::chemistryRole, "chemistryRole")
                .valueObject(description, "description")
                .notNull(this, AdultStage::nectarSources, "nectarSources");
    }

    /**
     * Adult feeding habit. NON_FEEDING adults (e.g. adult mayflies) have empty
     * nectarSources by definition; an invariant on this relationship could be
     * added when the enum stabilizes.
     */
    public enum FeedingHabit {
        NECTAR,
        SAP,
        HONEYDEW,
        POLLEN,
        PREDATORY,
        HEMATOPHAGOUS,
        NON_FEEDING,
        OMNIVOROUS
    }
}
