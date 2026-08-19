package com.naturalist.observation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.naturalist.data.FileName;
import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityId;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.RankName;
import com.naturalist.taxonomy.RankNameDeserializer;
import com.naturalist.taxonomy.RankNameSerializer;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A photographic record attached to an organism at a taxonomic rank — the shared
 * evidence unit across organism domains, and the photo sibling of
 * {@link OrganismObservation}. {@code parentName} is the taxon the photo identifies
 * (at whatever rank the naturalist's confidence allows); {@code observationId} links
 * the photo to the sighting it belongs to, and is null for a photo attached directly
 * to a catalog rank with no observation.
 *
 * <p>{@code parentName} is a domain permit ({@code InsectSpeciesName}, …) widened to
 * {@link RankName}; it serializes as a self-describing {@code {"rank":…,"value":…}}
 * object and rebuilds the concrete permit through the {@code RankNameReconstructor}
 * registered on the reading mapper (see each domain's image test-entity source).
 */
public record OrganismImage<IMG_ID extends EntityId, OBS_ID extends EntityId, R extends RankName>(
        IMG_ID id,
        @JsonSerialize(using = RankNameSerializer.class)
        @JsonDeserialize(using = RankNameDeserializer.class)
        R parentName,
        Instant dateAdded,
        FileName resourceName,
        @Nullable OBS_ID observationId
) implements Entity<IMG_ID> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(parentName, "parentName")
                .notNull(dateAdded, "dateAdded")
                .namedValue(resourceName, "resourceName")
                .whenNotNull(observationId, c -> c.entityId(observationId, "observationId"));
    }
}
