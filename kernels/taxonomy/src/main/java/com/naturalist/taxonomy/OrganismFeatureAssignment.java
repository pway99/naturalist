package com.naturalist.taxonomy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityId;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Binds a diagnostic {@code feature} to a taxonomic {@code rankName} at an
 * {@code ordinal} position — the shared feature-attachment unit across organism
 * domains. {@code featureId} references the domain's own feature record
 * (e.g. {@code InsectFeature}); the feature text is not stored here.
 *
 * <p>{@code rankName} is a domain permit ({@code InsectFamilyName}, …) widened to
 * {@link RankName}; it serialises as a self-describing {@code {"rank":…,"value":…}}
 * object and rebuilds the concrete permit through the {@code RankNameReconstructor}
 * registered on the reading mapper (see each domain's feature-assignment test source).
 */
public record OrganismFeatureAssignment<ID extends EntityId, FID extends EntityId, RANK extends RankName>(
        ID id,
        FID featureId,
        @JsonSerialize(using = RankNameSerializer.class)
        @JsonDeserialize(using = RankNameDeserializer.class)
        RANK rankName,
        int ordinal
) implements Entity<ID> {

    public static <ID extends EntityId, FID extends EntityId, RANK extends RankName>
    OrganismFeatureAssignment<ID, FID, RANK> of(ID id, FID featureId, RANK rankName, int ordinal) {
        return new OrganismFeatureAssignment<>(id, featureId, rankName, ordinal);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityId(featureId, "featureId")
                .identifier(rankName, "rankName");
    }
}
