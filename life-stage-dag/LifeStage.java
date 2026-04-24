package com.naturalist.insects.lifestages;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.identifiers.LifeStageKind;
import com.naturalist.identifiers.LifeStageName;
import org.jspecify.annotations.Nullable;

/**
 * A stage in an insect's life cycle. Sealed over four permitted subtypes.
 * Cross-stage invariants (chemistry coherence, metabolous-type consistency)
 * are enforced at the InsectSpecies aggregate root, not here.
 */
public sealed interface LifeStage extends NamedEntity<LifeStageName>
        permits EggStage, LarvaStage, PupaStage, AdultStage {

    LifeStageKind kind();

    StagePhenology phenology();

    StageHabitat habitat();

    @Nullable StageChemistryRole chemistryRole();

    Description description();
}
