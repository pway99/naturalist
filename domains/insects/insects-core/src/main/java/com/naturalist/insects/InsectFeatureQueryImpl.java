package com.naturalist.insects;

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
 * Lineage-composite feature resolution — the feature analog of
 * {@link InsectCitationQueryImpl}. Walks the ancestry chain from the subject
 * rank up to the order, gathers feature assignments at each rank, resolves
 * the {@link InsectFeature} entities, and composes the result as a
 * display-ready {@link InsectFeatureView} — one {@link InsectFeatureView.RankGroup}
 * per contributing rank, ancestor-first, ordinal-ordered within a group.
 */
class InsectFeatureQueryImpl implements InsectQuery.FeatureQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final InsectRepository.FeatureRepository featureRepository;
    private final InsectRepository.FeatureAssignmentRepository assignmentRepository;
    private final InsectAncestryResolver ancestryResolver;

    InsectFeatureQueryImpl(InsectRepository.FeatureRepository featureRepository,
                           InsectRepository.FeatureAssignmentRepository assignmentRepository,
                           InsectAncestryResolver ancestryResolver) {
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
    public InsectFeatureView findByRankName(InsectRankName subject) {
        observer.arguments("findByRankName", i -> i.identifier(subject, "subject"))
                .throwWhenInvalid();
        return findByAncestry(subject, ancestryResolver.ancestry(subject));
    }

    /**
     * The feature view for {@code subject}, resolved over a <em>pre-computed</em> ancestor-first
     * ancestry (order → … → subject) rather than re-walking the rank chain. The composed
     * {@code InsectFactory} resolves the lineage once and hands it here so the whole read model
     * costs a single ancestry walk; {@link #findByRankName} supplies the walk for direct callers.
     */
    InsectFeatureView findByAncestry(InsectRankName subject, Set<InsectRankName> ancestry) {
        observer.arguments("findByAncestry", i -> i
                        .identifier(subject, "subject")
                        .observableCollection(ancestry, "ancestry"))
                .throwWhenInvalid();

        // One batched fetch of every assignment across the whole ancestry.
        Map<InsectRankName, List<InsectFeatureAssignment>> byRank =
                assignmentRepository.getByRankNames(ancestry).stream()
                        .collect(Collectors.groupingBy(InsectFeatureAssignment::rankName));

        // One batched fetch of every referenced feature.
        Set<InsectFeatureId> allIds = byRank.values().stream().flatMap(List::stream)
                .map(InsectFeatureAssignment::featureId).collect(Collectors.toSet());
        Map<InsectFeatureId, InsectFeature> resolved = new HashMap<>();
        if (!allIds.isEmpty()) {
            for (InsectFeature f : featureRepository.getByEntityNameSet(allIds)) {
                resolved.put(f.id(), f);
            }
        }

        // Build groups ancestor-first via the ordered set, features ordinal-sorted per rank.
        List<InsectFeatureView.RankGroup> groups = new ArrayList<>();
        for (InsectRankName rank : ancestry) {
            List<InsectFeatureAssignment> atRank = byRank.getOrDefault(rank, List.of());
            if (atRank.isEmpty()) continue;
            List<InsectFeature> features = atRank.stream()
                    .sorted(Comparator.comparingInt(InsectFeatureAssignment::ordinal))
                    .map(x -> resolved.get(x.featureId()))
                    .filter(Objects::nonNull)
                    .toList();
            if (!features.isEmpty()) {
                groups.add(new InsectFeatureView.RankGroup(rank, features));
            }
        }

        InsectFeatureView view = new InsectFeatureView(subject, List.copyOf(groups));
        observer.observable(view, "featureView").observe(Level.WARN);
        return view;
    }

    @Override
    public Set<InsectRankName> findByFeature(InsectFeatureId featureId) {
        observer.arguments("findByFeature", i -> i.entityId(featureId, "featureId"))
                .throwWhenInvalid();

        return assignmentRepository.getByFeatureId(featureId).stream()
                .map(InsectFeatureAssignment::rankName)
                .collect(Collectors.toSet());
    }
}
