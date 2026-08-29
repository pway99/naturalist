package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.observation.OrganismImage;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.taxonomy.LinealRank;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Persistence view of an insect {@link OrganismImage} — a surrogate-UUID {@code Entity} keyed by
 * {@link InsectImageId}. Polymorphic {@link InsectRankName} parent stored as a {@code parent_rank}
 * discriminator + {@code parent_name} slug (no FK; up to 96 chars). {@code observationId} is a nullable
 * within-insects FK to {@code insect_observation}, stored as the uuid directly.
 */
@DboSchema(table = "insect_image", primaryKey = "id",
           foreignKeys = @Fk(columns = "observation_id", references = "insect_observation(id)"),
           entity = OrganismImage.class)
final class InsectImageDbo implements Dbo {
    String id;
    String parentRank;
    String parentName;
    Instant dateAdded;
    String resourceName;
    String observationId;  // nullable

    static InsectImageDbo from(OrganismImage<InsectImageId, InsectObservationId, InsectRankName> img) {
        InsectImageDbo d = new InsectImageDbo();
        d.id = img.id().value().toString();
        d.parentRank = img.parentName().rank().name();
        d.parentName = img.parentName().value();
        d.dateAdded = img.dateAdded();
        d.resourceName = img.resourceName().value();
        d.observationId = img.observationId() == null ? null : img.observationId().value().toString();
        Observer.forClass(InsectImageDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    OrganismImage<InsectImageId, InsectObservationId, InsectRankName> toEntity() {
        return new OrganismImage<>(
                InsectImageId.of(UUID.fromString(id)),
                InsectRankName.of(parentName, LinealRank.valueOf(parentRank)),
                dateAdded,
                FileName.of(resourceName),
                observationId == null ? null : InsectObservationId.of(UUID.fromString(observationId)));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notBlank(parentRank, "parentRank").maxLength(parentRank, 16, "parentRank")
                .notNull(parentName, "parentName").kebabFormat(parentName, "parentName")
                    .maxLength(parentName, 96, "parentName")
                .notNull(dateAdded, "dateAdded")
                .notBlank(resourceName, "resourceName").maxLength(resourceName, 255, "resourceName");
    }
}
