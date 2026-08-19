package com.naturalist.insects.lifestage;

import com.naturalist.observation.OrganismImage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectRankName;
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

    /**
     * The typed name of the catalogued rank (family, genus, or species) this
     * stage is attached to. Mirrors {@code OrganismImage.parentName}: the sealed
     * {@link InsectRankName} marker statically constrains the slot to insect-side
     * rank names. Jackson dispatch is declared on each permit's component, not
     * here, to keep the discriminator off direct leaf-class serialization.
     */
    InsectRankName parentName();

    StagePhenology phenology();

    StageHabitat habitat();

    @Nullable StageChemistryRole chemistryRole();

    Description description();
}
