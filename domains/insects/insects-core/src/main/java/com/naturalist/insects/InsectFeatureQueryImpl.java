package com.naturalist.insects;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lineage-composite feature resolution — the feature analog of
 * {@link InsectCitationQueryImpl}. Walks the ancestry chain from the subject
 * rank up to the order, gathers feature assignments at each rank, resolves
 * the {@link InsectFeature} entities, and composes the result as an
 * {@link InsectFeatureView} with ancestor-first ordering.
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
    public Optional<InsectFeatureView> findByRankName(InsectRankName subject) {
        observer.arguments("findByRankName", i -> i.identifier(subject, "subject"))
                .throwWhenInvalid();

        List<InsectRankName> ancestry = ancestryResolver.resolveAncestry(subject);

        // Gather all assignments across the ancestry, tagged with provenance.
        // Ancestry is already ordered subject-first → ancestor-last; we want
        // ancestor-first in the result, so we reverse the walk order.
        List<InsectFeatureAssignment> allAssignments = new ArrayList<>();
        for (int i = ancestry.size() - 1; i >= 0; i--) {
            InsectRankName rank = ancestry.get(i);
            List<InsectFeatureAssignment> atRank = assignmentRepository.getByRankName(rank);
            atRank.stream()
                    .sorted(Comparator.comparingInt(InsectFeatureAssignment::ordinal))
                    .forEach(allAssignments::add);
        }

        if (allAssignments.isEmpty()) {
            InsectFeatureView view = new InsectFeatureView(subject, List.of());
            observer.observable(view, "featureView").observe(Level.WARN);
            return Optional.of(view);
        }

        // Batch-resolve all referenced features.
        Set<InsectFeatureId> featureIds = allAssignments.stream()
                .map(InsectFeatureAssignment::featureId)
                .collect(Collectors.toSet());
        Map<InsectFeatureId, InsectFeature> resolved = new LinkedHashMap<>();
        for (InsectFeature f : featureRepository.getByEntityNameSet(featureIds)) {
            resolved.put(f.id(), f);
        }

        // Assemble RankedFeature entries.
        List<InsectFeatureView.RankedFeature> ranked = new ArrayList<>();
        for (InsectFeatureAssignment a : allAssignments) {
            InsectFeature feature = resolved.get(a.featureId());
            if (feature != null) {
                ranked.add(new InsectFeatureView.RankedFeature(feature, a.rankName(), a.ordinal()));
            }
        }

        InsectFeatureView view = new InsectFeatureView(subject, List.copyOf(ranked));
        observer.observable(view, "featureView").observe(Level.WARN);
        return Optional.of(view);
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
