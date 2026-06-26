package com.naturalist.insects;

import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * Lineage-composite view of identification features at a given rank — the insect
 * analog of {@link InsectCitationView} for citations.
 * <p>
 * The view composites the subject rank's own features with those inherited from its
 * ancestors via {@link InsectFeatureAssignment}. Each feature is tagged with its
 * source rank via {@link RankedFeature} so the consumer can group the display
 * ("Order-level marks: … / Family-level marks: …").
 * <p>
 * Ordering contract: ancestor ranks first (most general), descendant ranks last (most
 * specific). Within a rank, features are ordered by {@link RankedFeature#ordinal()}.
 * The composited list is therefore globally conspicuous→diagnostic.
 */
public record InsectFeatureView(
        InsectRankName subject,
        List<RankedFeature> features
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(subject, "subject")
                .notNull(features, "features");
    }

    /**
     * A resolved feature tagged with the rank that contributed it — provenance for
     * a single entry in the lineage-composite feature list.
     *
     * @param feature    the resolved {@link InsectFeature} entity
     * @param assignedAt which rank in the lineage contributed this feature
     * @param ordinal    position in the conspicuous-to-diagnostic ordering for that rank
     */
    public record RankedFeature(
            InsectFeature feature,
            InsectRankName assignedAt,
            int ordinal
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .notNull(feature, "feature")
                    .identifier(assignedAt, "assignedAt");
        }
    }
}
