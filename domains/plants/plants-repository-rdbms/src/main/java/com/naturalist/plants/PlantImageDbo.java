package com.naturalist.plants;

import com.naturalist.data.FileName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.observation.OrganismImage;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.taxonomy.LinealRank;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Persistence view of a plant {@link OrganismImage} — a surrogate-UUID {@code Entity} keyed by
 * {@link PlantImageId}. Its {@code parentName} is a polymorphic {@link PlantRankName} stored as a
 * {@code parent_rank} discriminator + {@code parent_name} slug (no FK — spans four rank tables) and
 * rebuilt in-module. {@code observationId} is a nullable within-plants FK to {@code plant_observation}
 * (a surrogate-UUID entity), stored as the uuid directly. Uuid columns travel as text with a
 * {@code ::uuid} cast (no MyBatis UUID handler).
 */
@DboSchema(table = "plant_image", primaryKey = "id",
           foreignKeys = @Fk(columns = "observation_id", references = "plant_observation(id)"),
           entity = OrganismImage.class)
final class PlantImageDbo implements Dbo {
    String id;             // the PlantImageId's UUID as text
    String parentRank;     // ORDER|FAMILY|GENUS|SPECIES
    String parentName;     // rank-name slug (polymorphic, no FK)
    java.time.Instant dateAdded;
    String resourceName;   // FileName value (filename only)
    String observationId;  // nullable FK to plant_observation(id)

    static PlantImageDbo from(OrganismImage<PlantImageId, PlantObservationId, PlantRankName> img) {
        PlantImageDbo d = new PlantImageDbo();
        d.id = img.id().value().toString();
        d.parentRank = img.parentName().rank().name();
        d.parentName = img.parentName().value();
        d.dateAdded = img.dateAdded();
        d.resourceName = img.resourceName().value();
        d.observationId = img.observationId() == null ? null : img.observationId().value().toString();
        Observer.forClass(PlantImageDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    OrganismImage<PlantImageId, PlantObservationId, PlantRankName> toEntity() {
        return new OrganismImage<>(
                PlantImageId.of(UUID.fromString(id)),
                PlantRankName.of(parentName, LinealRank.valueOf(parentRank)),
                dateAdded,
                FileName.of(resourceName),
                observationId == null ? null : PlantObservationId.of(UUID.fromString(observationId)));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notBlank(parentRank, "parentRank").maxLength(parentRank, 16, "parentRank")
                .notNull(parentName, "parentName").kebabFormat(parentName, "parentName")
                    .maxLength(parentName, 64, "parentName")
                .notNull(dateAdded, "dateAdded")
                .notBlank(resourceName, "resourceName").maxLength(resourceName, 255, "resourceName");
    }
}
