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
import com.naturalist.plants.PlantSpeciesName;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The adult stage. Flight timing lives in {@link StagePhenology}, not as a
 * separate field here.
 * <p>
 * {@code nectarSources} is typed as {@code List<PlantSpeciesName>} — nectar is
 * produced only by flowering plants. Non-nectar adult feeding (sap, carrion,
 * blood, honeydew) is not modeled here; when needed, each becomes a separate
 * typed field (e.g. {@code sapSources}), not a generalization of nectar.
 * <p>
 * Adult predation (robber flies, mantids) produces no prey field — same reason
 * as {@link LarvaStage}: prey relationships live in a future ecology domain.
 */
public record AdultStage(
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
        @Nullable FeedingHabit feedingHabit,
        List<PlantSpeciesName> nectarSources,
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
                .identifier(parentName, "parentName")
                .valueObject(phenology, "phenology")
                .valueObject(habitat, "habitat")
                .valueObjectOrNull(chemistryRole, "chemistryRole")
                .valueObject(description, "description")
                .notNull(nectarSources, "nectarSources");
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
