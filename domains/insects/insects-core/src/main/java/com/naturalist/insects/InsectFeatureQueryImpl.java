package com.naturalist.insects;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

        List<InsectRankName> ancestry = ancestryResolver.resolveAncestry(subject); // subject-first
        Map<InsectRankName, List<InsectFeatureAssignment>> assignmentsByRank = new LinkedHashMap<>();

        // Ancestor-first: walk the ancestry in reverse (order → … → subject).
        for (int a = ancestry.size() - 1; a >= 0; a--) {
            InsectRankName rank = ancestry.get(a);
            List<InsectFeatureAssignment> atRank = assignmentRepository.getByRankName(rank).stream()
                    .sorted(Comparator.comparingInt(InsectFeatureAssignment::ordinal))
                    .toList();
            if (atRank.isEmpty()) {
                continue;
            }
            assignmentsByRank.put(rank, atRank);
        }

        Set<InsectFeatureId> allIds = assignmentsByRank.values().stream()
                .flatMap(List::stream)
                .map(InsectFeatureAssignment::featureId)
                .collect(Collectors.toSet());
        Map<InsectFeatureId, InsectFeature> resolved = new LinkedHashMap<>();
        if (!allIds.isEmpty()) {
            // single batched fetch across the ancestry
            for (InsectFeature f : featureRepository.getByEntityNameSet(allIds)) {
                resolved.put(f.id(), f);
            }
        }

        List<InsectFeatureView.RankGroup> groups = new ArrayList<>();
        for (Map.Entry<InsectRankName, List<InsectFeatureAssignment>> entry : assignmentsByRank.entrySet()) {
            List<InsectFeature> features = entry.getValue().stream()
                    .map(x -> resolved.get(x.featureId()))
                    .filter(Objects::nonNull)
                    .toList();
            if (!features.isEmpty()) {
                groups.add(new InsectFeatureView.RankGroup(entry.getKey(), features));
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
