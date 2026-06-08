package com.naturalist.insects.lifestage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.ddd.ValueObject;
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
 * The pupal stage. Present only on holometabolous species; null on the species
 * aggregate for hemimetabolous orders (Orthoptera, Hemiptera, Blattodea).
 * <p>
 * Direct-developer vs. diapauser cohort timing lives in {@link StagePhenology}
 * as multiple {@link StagePhenology.ActivityWindow}s. The pupa's
 * {@link DiapauseRegulation} describes the mechanism; phenology describes the
 * resulting timing.
 */
public record PupaStage(
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
        @Nullable String appearance,
        @Nullable DiapauseRegulation diapauseRegulation,
        @Nullable String adaptiveSignificance
) implements LifeStage {

    @Override
    public LifeStageKind kind() {
        return LifeStageKind.PUPA;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .identifier(parentName, "parentName")
                .valueObject(phenology, "phenology")
                .valueObject(habitat, "habitat")
                .valueObjectOrNull(chemistryRole, "chemistryRole")
                .valueObject(description, "description")
                .valueObjectOrNull(diapauseRegulation, "diapauseRegulation");
    }

    /**
     * Mechanism governing pupal diapause. Sealed because the mechanism space is
     * closed but structurally heterogeneous.
     */
    public sealed interface DiapauseRegulation extends ValueObject
            permits PhotoperiodRegulated,
            FoodWaterContentRegulated,
            TemperatureRegulated,
            NonDiapausing {
    }

    public record PhotoperiodRegulated(
            @Nullable String criticalDaylength,
            @Nullable String chillRequirement
    ) implements DiapauseRegulation {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {
            };
        }
    }

    /**
     * The Battus philenor case. Larval food moisture at pupation determines
     * direct development vs. diapause. Produces INDETERMINATE voltinism.
     */
    public record FoodWaterContentRegulated(
            @Nullable String mechanism,
            @Nullable String cohortSplitNotes
    ) implements DiapauseRegulation {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {
            };
        }
    }

    public record TemperatureRegulated(
            @Nullable String entryThreshold,
            @Nullable String exitThreshold
    ) implements DiapauseRegulation {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {
            };
        }
    }

    /**
     * Positively documented as not diapausing. Distinct from null
     * diapauseRegulation, which means "not yet documented."
     */
    public record NonDiapausing() implements DiapauseRegulation {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {
            };
        }
    }
}
