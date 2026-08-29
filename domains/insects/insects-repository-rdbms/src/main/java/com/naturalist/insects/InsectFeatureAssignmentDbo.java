package com.naturalist.insects;

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
 * Persistence view of an insect feature assignment — the kernel generic
 * {@link OrganismFeatureAssignment} keyed by {@link InsectFeatureAssignmentId}. Two uuid columns
 * store as text and are cast in SQL ({@code ::uuid}): the surrogate {@code id} and the
 * {@code feature_id}, a within-insects FK to {@code insect_feature(id)}. Because {@code insect_feature}
 * is itself a UUID entity, the feature reference is stored directly and FK-enforced — no name
 * indirection, no JOIN.
 *
 * <p>The polymorphic {@link InsectRankName} splits into a {@code rank} discriminator
 * ({@link LinealRank#name()}) plus a {@code rank_name} slug, rebuilt in-module via
 * {@link InsectRankName#of(String, LinealRank)} (five permits, including {@code SUBSPECIES}). The
 * {@code rank_name} column is {@code VARCHAR(96)} — wider than plants' 64 — because insect
 * subspecies slugs reach 96 chars. The {@code UNIQUE(feature_id, rank_name)} is a composite
 * constraint — enforced by the DDL only, deliberately omitted from {@code @DboSchema.unique}
 * (the validator checks single-column uniques).
 */
@DboSchema(table = "insect_feature_assignment", primaryKey = "id",
           foreignKeys = @Fk(columns = "feature_id", references = "insect_feature(id)"),
           entity = OrganismFeatureAssignment.class)
final class InsectFeatureAssignmentDbo implements Dbo {
    String id;         // the InsectFeatureAssignmentId's UUID as text; mapper casts it (::uuid)
    String featureId;  // the InsectFeatureId's UUID as text — within-insects FK, cast (::uuid)
    String rank;       // ORDER|FAMILY|GENUS|SPECIES|SUBSPECIES — rankName.rank().name()
    String rankName;   // rank-name slug (polymorphic, no FK) — rankName.value()
    int ordinal;

    static InsectFeatureAssignmentDbo from(
            OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> a) {
        InsectFeatureAssignmentDbo d = new InsectFeatureAssignmentDbo();
        d.id = a.id().value().toString();
        d.featureId = a.featureId().value().toString();
        d.rank = a.rankName().rank().name();
        d.rankName = a.rankName().value();
        d.ordinal = a.ordinal();
        Observer.forClass(InsectFeatureAssignmentDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> toEntity() {
        return OrganismFeatureAssignment.of(
                InsectFeatureAssignmentId.of(UUID.fromString(id)),
                InsectFeatureId.of(UUID.fromString(featureId)),
                InsectRankName.of(rankName, LinealRank.valueOf(rank)),
                ordinal);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notBlank(featureId, "featureId")
                .notBlank(rank, "rank").maxLength(rank, 16, "rank")
                .notNull(rankName, "rankName").kebabFormat(rankName, "rankName").maxLength(rankName, 96, "rankName");
    }
}
