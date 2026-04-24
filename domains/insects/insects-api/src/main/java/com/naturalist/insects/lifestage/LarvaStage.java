package com.naturalist.insects.lifestage;

import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectSpeciesName;
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
                .valueObject(phenology, "phenology")
                .valueObject(habitat, "habitat")
                .valueObjectOrNull(this, LarvaStage::chemistryRole, "chemistryRole")
                .valueObject(description, "description")
                .notNull(this, LarvaStage::hostPlants, "hostPlants")
                .notNull(this, LarvaStage::parasitoidHosts, "parasitoidHosts");
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
