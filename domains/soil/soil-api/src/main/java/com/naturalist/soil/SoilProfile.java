package com.naturalist.soil;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;
import com.naturalist.soil.observation.LabAnalysis;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The complete soil profile — a {@link ReadModel} tying together the persisted {@link SoilProfileInfo}
 * root and the profile's assembled {@link LabAnalysis} analyses (each carrying its nutrient panel and
 * physical characteristics), most recent last. Composed on read by {@code SoilProfileFactory} from the
 * persisted parts; never stored.
 * <p>
 * Amendment / irrigation / tillage / precipitation histories and the current mulch layer are a
 * separate effort and are not part of this projection yet.
 */
public record SoilProfile(
        SoilProfileInfo info,
        List<LabAnalysis> labAnalyses
) implements ReadModel {

    /** The natural key of this profile. */
    public SoilProfileName soilProfileName() {
        return info.name();
    }

    /** The most recent analysis on record, if any. */
    public Optional<LabAnalysis> latestLabAnalysis() {
        if (labAnalyses.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(labAnalyses.get(labAnalyses.size() - 1));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(info, "info")
                .notNull(labAnalyses, "labAnalyses");
    }
}
