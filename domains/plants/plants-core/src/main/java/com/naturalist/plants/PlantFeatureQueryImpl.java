package com.naturalist.plants;

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

/**
 * Lineage-composite feature resolution — walks the ancestry chain from the subject rank up
 * to the order, gathers feature assignments at each rank in one batched call, resolves the
 * {@link PlantFeature} entities in one batched call, and delegates grouping to the kernel's
 * {@link FeatureViewAssembler}, composing the result as a display-ready
 * {@link OrganismFeatureView} — one {@link OrganismFeatureView.RankGroup} per contributing
 * rank, ancestor-first, ordinal-ordered within a group. Mirrors {@code InsectFeatureQueryImpl}.
 */
@DomainService
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
    public OrganismFeatureView<PlantRankName, PlantFeature> findByRankName(PlantRankName subject) {
        observer.arguments("findByRankName", i -> i.identifier(subject, "subject")).throwWhenInvalid();
        return findByAncestry(subject, ancestryResolver.ancestry(subject));
    }

    /**
     * The feature view for {@code subject}, resolved over a <em>pre-computed</em> ancestor-first
     * ancestry (order → … → subject) rather than re-walking the rank chain. The composed
     * {@code PlantFactory} resolves the lineage once and hands it here so the whole read model
     * costs a single ancestry walk; {@link #findByRankName} supplies the walk for direct callers.
     */
    OrganismFeatureView<PlantRankName, PlantFeature> findByAncestry(PlantRankName subject, Set<PlantRankName> ancestry) {
        observer.arguments("findByAncestry", i -> i
                        .identifier(subject, "subject")
                        .observableCollection(ancestry, "ancestry"))
                .throwWhenInvalid();

        List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> assignments =
                assignmentRepository.getByRankNames(ancestry);   // batch 1

        Set<PlantFeatureId> allIds = assignments.stream()
                .map(OrganismFeatureAssignment::featureId).collect(Collectors.toSet());
        Map<PlantFeatureId, PlantFeature> resolved = new HashMap<>();
        if (!allIds.isEmpty()) {
            for (PlantFeature f : featureRepository.getByEntityNameSet(allIds)) {   // batch 2
                resolved.put(f.id(), f);
            }
        }

        OrganismFeatureView<PlantRankName, PlantFeature> view =
                FeatureViewAssembler.assemble(subject, ancestry, assignments, resolved);
        observer.observable(view, "featureView").observe(Level.WARN);
        return view;
    }
}
