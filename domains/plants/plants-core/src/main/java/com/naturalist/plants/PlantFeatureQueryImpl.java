package com.naturalist.plants;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lineage-composite feature resolution — walks the ancestry chain from the subject rank up
 * to the order, gathers feature assignments at each rank in one batched call, resolves the
 * {@link PlantFeature} entities in one batched call, and composes a display-ready
 * {@link PlantFeatureView} — one {@link PlantFeatureView.RankGroup} per contributing rank,
 * ancestor-first, ordinal-ordered within a group. Mirrors {@code InsectFeatureQueryImpl}.
 */
class PlantFeatureQueryImpl implements PlantQuery.FeatureQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final PlantRepository.FeatureRepository featureRepository;
    private final PlantRepository.FeatureAssignmentRepository assignmentRepository;
    private final PlantAncestryResolver ancestryResolver;

    PlantFeatureQueryImpl(PlantRepository.FeatureRepository featureRepository,
                          PlantRepository.FeatureAssignmentRepository assignmentRepository,
                          PlantAncestryResolver ancestryResolver) {
        observer.arguments("constructor", i -> i
                        .notNull(featureRepository, "featureRepository")
                        .notNull(assignmentRepository, "assignmentRepository")
                        .notNull(ancestryResolver, "ancestryResolver"))
                .throwWhenInvalid();
        this.featureRepository = featureRepository;
        this.assignmentRepository = assignmentRepository;
        this.ancestryResolver = ancestryResolver;
    }

    @Override
    public PlantFeatureView findByRankName(PlantRankName subject) {
        observer.arguments("findByRankName", i -> i.identifier(subject, "subject")).throwWhenInvalid();

        Set<PlantRankName> ancestry = ancestryResolver.ancestry(subject); // ancestor-first, ordered

        Map<PlantRankName, List<PlantFeatureAssignment>> byRank =
                assignmentRepository.getByRankNames(ancestry).stream()
                        .collect(Collectors.groupingBy(PlantFeatureAssignment::rankName));

        Set<PlantFeatureId> allIds = byRank.values().stream().flatMap(List::stream)
                .map(PlantFeatureAssignment::featureId).collect(Collectors.toSet());
        Map<PlantFeatureId, PlantFeature> resolved = new HashMap<>();
        if (!allIds.isEmpty()) {
            for (PlantFeature f : featureRepository.getByEntityNameSet(allIds)) {
                resolved.put(f.id(), f);
            }
        }

        List<PlantFeatureView.RankGroup> groups = new ArrayList<>();
        for (PlantRankName rank : ancestry) {
            List<PlantFeatureAssignment> atRank = byRank.getOrDefault(rank, List.of());
            if (atRank.isEmpty()) continue;
            List<PlantFeature> features = atRank.stream()
                    .sorted(Comparator.comparingInt(PlantFeatureAssignment::ordinal))
                    .map(x -> resolved.get(x.featureId()))
                    .filter(Objects::nonNull)
                    .toList();
            if (!features.isEmpty()) {
                groups.add(new PlantFeatureView.RankGroup(rank, features));
            }
        }

        PlantFeatureView view = new PlantFeatureView(subject, List.copyOf(groups));
        observer.observable(view, "featureView").observe(Level.WARN);
        return view;
    }
}
