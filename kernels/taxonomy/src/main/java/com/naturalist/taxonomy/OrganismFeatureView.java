package com.naturalist.taxonomy;

import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * The feature marks that apply to a taxon, grouped by the ancestor rank that
 * contributed them, ancestor-first. {@code FEATURE} is the domain's own feature
 * record; the view holds it structurally without constraining its type.
 */
public record OrganismFeatureView<RANK extends RankName, FEATURE>(
        RANK subject,
        List<RankGroup<RANK, FEATURE>> groups
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(subject, "subject")
                .notNull(groups, "groups");
    }

    /** The feature marks contributed at one rank in the lineage, ordinal-ordered. */
    public record RankGroup<RANK extends RankName, FEATURE>(
            RANK rank,
            List<FEATURE> features
    ) implements ValueObject {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .identifier(rank, "rank")
                    .notNull(features, "features");
        }
    }
}
