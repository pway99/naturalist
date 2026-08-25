package com.naturalist.insects;

import com.naturalist.data.Pages;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.FeatureViewAssembler;
import com.naturalist.taxonomy.OrganismFeatureAssignment;
import com.naturalist.taxonomy.OrganismFeatureView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lineage-composite feature resolution — the feature analog of
 * {@link InsectCitationQueryImpl}. Walks the ancestry chain from the subject
 * rank up to the order, gathers feature assignments at each rank, resolves
 * the {@link InsectFeature} entities, and delegates grouping to the kernel's
 * {@link FeatureViewAssembler}, composing the result as a display-ready
 * {@link OrganismFeatureView} — one
 * {@link OrganismFeatureView.RankGroup} per contributing rank, ancestor-first,
 * ordinal-ordered within a group.
 */
@DomainService
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
    public OrganismFeatureView<InsectRankName, InsectFeature> findByRankName(InsectRankName subject) {
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
    OrganismFeatureView<InsectRankName, InsectFeature> findByAncestry(InsectRankName subject, Set<InsectRankName> ancestry) {
        observer.arguments("findByAncestry", i -> i
                        .identifier(subject, "subject")
                        .observableCollection(ancestry, "ancestry"))
                .throwWhenInvalid();

        List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> assignments =
                assignmentRepository.getByRankNames(ancestry);   // batch 1

        Set<InsectFeatureId> allIds = assignments.stream()
                .map(OrganismFeatureAssignment::featureId).collect(Collectors.toSet());
        Map<InsectFeatureId, InsectFeature> resolved = new HashMap<>();
        if (!allIds.isEmpty()) {
            for (InsectFeature f : featureRepository.getByEntityNameSet(allIds)) {   // batch 2
                resolved.put(f.id(), f);
            }
        }

        OrganismFeatureView<InsectRankName, InsectFeature> view =
                FeatureViewAssembler.assemble(subject, ancestry, assignments, resolved);
        observer.observable(view, "featureView").observe(Level.WARN);
        return view;
    }

    @Override
    public Set<InsectRankName> findByFeature(InsectFeatureId featureId) {
        observer.arguments("findByFeature", i -> i.entityId(featureId, "featureId"))
                .throwWhenInvalid();

        return assignmentRepository.getByFeatureId(featureId).stream()
                .map(OrganismFeatureAssignment::rankName)
                .collect(Collectors.toSet());
    }

    @Override
    public Stream<InsectFeature> corpus() {
        return Pages.stream(1000, featureRepository::getPage);
    }
}
