package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The many-to-many link between a {@link PlantFeature} and a Linnaean rank.
 * <p>
 * Each assignment attaches one feature to one rank with a position in the
 * conspicuous-to-diagnostic ordering. The pair {@code (featureId, rankName)}
 * is unique — a feature can only be assigned to a given rank once.
 * <p>
 * {@code ordinal} is per-rank ordering, not global. The assignment at family
 * Asteraceae has its own ordinal sequence independent of the assignment at
 * genus Salvia.
 * <p>
 * Jackson dispatch on {@code rankName} mirrors {@code insects.InsectFeatureAssignment}:
 * field-level {@code @JsonTypeInfo} with {@link As#EXTERNAL_PROPERTY} flattens the
 * discriminator into a sibling {@code "rank"} field, keeping {@code rankName}
 * itself a plain slug string via {@code EntityName}'s {@code @JsonValue}. Only
 * four permits — plants has no {@code PlantSubspeciesName}.
 */
public record PlantFeatureAssignment(
        PlantFeatureAssignmentId id,
        PlantFeatureId featureId,
        @JsonTypeInfo(use = Id.NAME, property = "rank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = PlantOrderName.class, name = "ORDER"),
                @Type(value = PlantFamilyName.class, name = "FAMILY"),
                @Type(value = PlantGenusName.class, name = "GENUS"),
                @Type(value = PlantSpeciesName.class, name = "SPECIES")
        })
        PlantRankName rankName,
        int ordinal
) implements Entity<PlantFeatureAssignmentId> {

    public static PlantFeatureAssignment of(PlantFeatureAssignmentId id,
                                             PlantFeatureId featureId,
                                             PlantRankName rankName,
                                             int ordinal) {
        return new PlantFeatureAssignment(id, featureId, rankName, ordinal);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityId(featureId, "featureId")
                .identifier(rankName, "rankName");
    }
}
