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
import com.naturalist.plants.PlantName;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The larval stage — feeding, growth, instars.
 * <p>
 * {@code hostPlants} covers phytophagous specialization (Battus philenor →
 * Aristolochia californica). Catalog-level species trait.
 * <p>
 * {@code parasitoidHosts} covers hymenopteran/dipteran parasitoids whose hosts
 * are other insect species. Catalog-level trait.
 * <p>
 * Prey for predatory larvae is deliberately NOT a field here. Prey relationships
 * are relationship entities in a future ecology domain (see LifeStage.md §8).
 * {@code feedingStrategy} carries the strategy label so the stage can be
 * classified without the prey list.
 */
public record LarvaStage(
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
        @Nullable FeedingStrategy feedingStrategy,
        List<PlantName> hostPlants,
        List<InsectSpeciesName> parasitoidHosts,
        @Nullable String remarkableBehavior,
        @Nullable String instarProgression
) implements LifeStage {

    @Override
    public LifeStageKind kind() {
        return LifeStageKind.LARVA;
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
                .notNull(hostPlants, "hostPlants")
                .notNull(parasitoidHosts, "parasitoidHosts");
    }

    /**
     * Broad feeding strategy classification. PREDATORY larvae have no prey field
     * on this record; prey relationships live in the future ecology domain.
     */
    public enum FeedingStrategy {
        PHYTOPHAGOUS,
        PREDATORY,
        PARASITOID,
        DETRITIVORE,
        OMNIVOROUS
    }
}
