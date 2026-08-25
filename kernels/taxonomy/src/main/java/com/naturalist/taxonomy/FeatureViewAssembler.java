package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds an {@link OrganismFeatureView} from pre-fetched assignments and features.
 * Pure and repository-free so it can live in the kernel: the caller performs the
 * two batched reads (assignments across the ancestry, features by id) and passes
 * the results. Group order follows {@code ancestry} iteration order (ancestor-first);
 * within a rank, features are ordinal-sorted; empty ranks and unresolved ids drop out.
 */
public final class FeatureViewAssembler {

    private FeatureViewAssembler() {
    }

    public static <RANK extends RankName, FID extends EntityId, FEATURE>
    OrganismFeatureView<RANK, FEATURE> assemble(
            RANK subject,
            Set<RANK> ancestry,
            List<? extends OrganismFeatureAssignment<?, FID, RANK>> assignments,
            Map<FID, FEATURE> resolved) {

        Map<RANK, List<OrganismFeatureAssignment<?, FID, RANK>>> byRank = new HashMap<>();
        for (OrganismFeatureAssignment<?, FID, RANK> a : assignments) {
            byRank.computeIfAbsent(a.rankName(), k -> new ArrayList<>()).add(a);
        }

        List<OrganismFeatureView.RankGroup<RANK, FEATURE>> groups = new ArrayList<>();
        for (RANK rank : ancestry) {
            List<OrganismFeatureAssignment<?, FID, RANK>> atRank = byRank.getOrDefault(rank, List.of());
            if (atRank.isEmpty()) continue;
            List<FEATURE> features = atRank.stream()
                    .sorted(Comparator.comparingInt(OrganismFeatureAssignment::ordinal))
                    .map(a -> resolved.get(a.featureId()))
                    .filter(Objects::nonNull)
                    .toList();
            if (!features.isEmpty()) {
                groups.add(new OrganismFeatureView.RankGroup<>(rank, features));
            }
        }
        return new OrganismFeatureView<>(subject, List.copyOf(groups));
    }
}
