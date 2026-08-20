package com.naturalist.insects;

import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * Lineage-composite, display-ready view of identification features at a given rank —
 * the insect analog of {@link InsectCitationView} for citations.
 * <p>
 * The view composites the subject rank's own features with those inherited from its
 * ancestors via {@link InsectFeatureAssignment}, pre-grouped by contributing rank so
 * the console can render directly ("Order-level marks: … / Family-level marks: …")
 * without reshaping the data itself.
 * <p>
 * Ordering contract: ancestor groups first (most general), descendant groups last
 * (most specific). Within a group, features are ordered by their assignment ordinal.
 * The composited groups are therefore globally conspicuous→diagnostic.
 */
public record InsectFeatureView(
        InsectRankName subject,
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
            InsectRankName rank,
            List<InsectFeature> features
    ) implements ValueObject {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .identifier(rank, "rank")
                    .notNull(features, "features");
        }
    }
}
