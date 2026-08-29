package com.naturalist.plants;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.taxonomy.LinealRank;
import com.naturalist.taxonomy.OrganismFeatureAssignment;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Persistence view of a plant feature assignment — the kernel generic
 * {@link OrganismFeatureAssignment} keyed by {@link PlantFeatureAssignmentId}. Two uuid columns
 * store as text and are cast in SQL ({@code ::uuid}): the surrogate {@code id} and the
 * {@code feature_id}, a within-plants FK to {@code plant_feature(id)}. Because {@code plant_feature}
 * is itself a UUID entity, the feature reference is stored directly and FK-enforced — no name
 * indirection, no JOIN.
 *
 * <p>The polymorphic {@link PlantRankName} splits into a {@code rank} discriminator
 * ({@link LinealRank#name()}) plus a {@code rank_name} slug, rebuilt in-module via
 * {@link PlantRankName#of(String, LinealRank)}. The {@code UNIQUE(feature_id, rank_name)} is a
 * composite constraint — enforced by the DDL only, deliberately omitted from {@code @DboSchema.unique}
 * (the validator checks single-column uniques).
 */
@DboSchema(table = "plant_feature_assignment", primaryKey = "id",
           foreignKeys = @Fk(columns = "feature_id", references = "plant_feature(id)"),
           entity = OrganismFeatureAssignment.class)
final class PlantFeatureAssignmentDbo implements Dbo {
    String id;         // the PlantFeatureAssignmentId's UUID as text; mapper casts it (::uuid)
    String featureId;  // the PlantFeatureId's UUID as text — within-plants FK, cast (::uuid)
    String rank;       // ORDER|FAMILY|GENUS|SPECIES — rankName.rank().name()
    String rankName;   // rank-name slug (polymorphic, no FK) — rankName.value()
    int ordinal;

    static PlantFeatureAssignmentDbo from(
            OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> a) {
        PlantFeatureAssignmentDbo d = new PlantFeatureAssignmentDbo();
        d.id = a.id().value().toString();
        d.featureId = a.featureId().value().toString();
        d.rank = a.rankName().rank().name();
        d.rankName = a.rankName().value();
        d.ordinal = a.ordinal();
        Observer.forClass(PlantFeatureAssignmentDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> toEntity() {
        return OrganismFeatureAssignment.of(
                PlantFeatureAssignmentId.of(UUID.fromString(id)),
                PlantFeatureId.of(UUID.fromString(featureId)),
                PlantRankName.of(rankName, LinealRank.valueOf(rank)),
                ordinal);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notBlank(featureId, "featureId")
                .notBlank(rank, "rank").maxLength(rank, 16, "rank")
                .notNull(rankName, "rankName").kebabFormat(rankName, "rankName").maxLength(rankName, 64, "rankName");
    }
}
