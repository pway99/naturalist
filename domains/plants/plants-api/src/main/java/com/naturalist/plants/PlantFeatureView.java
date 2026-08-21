package com.naturalist.plants;

import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * Lineage-composite, display-ready view of identification features at a given rank.
 * Composites the subject rank's own field marks with those inherited from its ancestors,
 * pre-grouped by contributing rank so the console can render directly without reshaping.
 * <p>
 * Ordering contract: ancestor groups first (most general), descendant groups last (most
 * specific). Within a group, features are ordered by assignment ordinal. Mirrors
 * {@code InsectFeatureView}.
 */
public record PlantFeatureView(
        PlantRankName subject,
        List<RankGroup> groups
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(subject, "subject")
                .notNull(groups, "groups");
    }

    /** The field marks contributed at one rank in the lineage, ordinal-ordered. */
    public record RankGroup(
            PlantRankName rank,
            List<PlantFeature> features
    ) implements ValueObject {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .identifier(rank, "rank")
                    .notNull(features, "features");
        }
    }
}
