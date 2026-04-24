package com.naturalist.insects.lifestage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.LifeStageKind;
import com.naturalist.insects.LifeStageName;
import org.jspecify.annotations.Nullable;

/**
 * A stage in an insect's life cycle. Sealed over four permitted subtypes.
 * Cross-stage invariants (chemistry coherence, metabolous-type consistency)
 * are enforced at the InsectSpecies aggregate root, not here.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EggStage.class, name = "EGG"),
        @JsonSubTypes.Type(value = LarvaStage.class, name = "LARVA"),
        @JsonSubTypes.Type(value = PupaStage.class, name = "PUPA"),
        @JsonSubTypes.Type(value = AdultStage.class, name = "ADULT")
})
public sealed interface LifeStage extends NamedEntity<LifeStageName>
        permits EggStage, LarvaStage, PupaStage, AdultStage {

    LifeStageKind kind();

    StagePhenology phenology();

    StageHabitat habitat();

    @Nullable StageChemistryRole chemistryRole();

    Description description();
}
