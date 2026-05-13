package com.naturalist.insects.lifestage;

import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.LifeStageKind;
import com.naturalist.insects.LifeStageName;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The egg stage. Present on every insect species.
 */
public record EggStage(
        LifeStageName name,
        StagePhenology phenology,
        StageHabitat habitat,
        @Nullable StageChemistryRole chemistryRole,
        Description description,
        @Nullable String colorProgression,
        @Nullable String layingPattern,
        @Nullable String adaptiveSignificance
) implements LifeStage {

    @Override
    public LifeStageKind kind() {
        return LifeStageKind.EGG;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .valueObject(phenology, "phenology")
                .valueObject(habitat, "habitat")
                .valueObjectOrNull(chemistryRole, "chemistryRole")
                .valueObject(description, "description");
    }
}
