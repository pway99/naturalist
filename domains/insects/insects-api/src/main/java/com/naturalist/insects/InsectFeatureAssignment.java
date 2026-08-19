package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The many-to-many link between an {@link InsectFeature} and a Linnaean rank.
 * <p>
 * Each assignment attaches one feature to one rank with a position in the
 * conspicuous-to-diagnostic ordering. The pair {@code (featureId, rankName)}
 * is unique — a feature can only be assigned to a given rank once.
 * <p>
 * {@code ordinal} is per-rank ordering, not global. The assignment at order
 * Hemiptera has its own ordinal sequence independent of the assignment at
 * family Cicadellidae.
 * <p>
 * Jackson dispatch on {@code rankName} mirrors {@link OrganismImage}: field-level
 * {@code @JsonTypeInfo} with {@link As#EXTERNAL_PROPERTY} flattens the
 * discriminator into a sibling {@code "rank"} field, keeping {@code rankName}
 * itself a plain slug string via {@code EntityName}'s {@code @JsonValue}.
 */
public record InsectFeatureAssignment(
        InsectFeatureAssignmentId id,
        InsectFeatureId featureId,
        @JsonTypeInfo(use = Id.NAME, property = "rank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = InsectOrderName.class, name = "ORDER"),
                @Type(value = InsectFamilyName.class, name = "FAMILY"),
                @Type(value = InsectGenusName.class, name = "GENUS"),
                @Type(value = InsectSpeciesName.class, name = "SPECIES"),
                @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
        })
        InsectRankName rankName,
        int ordinal
) implements Entity<InsectFeatureAssignmentId> {

    public static InsectFeatureAssignment of(InsectFeatureAssignmentId id,
                                             InsectFeatureId featureId,
                                             InsectRankName rankName,
                                             int ordinal) {
        return new InsectFeatureAssignment(id, featureId, rankName, ordinal);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityId(featureId, "featureId")
                .identifier(rankName, "rankName");
    }
}
