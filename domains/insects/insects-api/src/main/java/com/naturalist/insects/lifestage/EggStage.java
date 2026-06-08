package com.naturalist.insects.lifestage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.InsectSubspeciesName;
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
        @JsonTypeInfo(use = Id.NAME, property = "parentRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = InsectOrderName.class, name = "ORDER"),
                @Type(value = InsectFamilyName.class, name = "FAMILY"),
                @Type(value = InsectGenusName.class, name = "GENUS"),
                @Type(value = InsectSpeciesName.class, name = "SPECIES"),
                @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
        })
        InsectRankName parentName,
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
                .identifier(parentName, "parentName")
                .valueObject(phenology, "phenology")
                .valueObject(habitat, "habitat")
                .valueObjectOrNull(chemistryRole, "chemistryRole")
                .valueObject(description, "description");
    }
}
