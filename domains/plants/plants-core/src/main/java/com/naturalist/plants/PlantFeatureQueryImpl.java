package com.naturalist.plants;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantEntityCollections.FeatureCollection;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Direct-rank feature resolution — the application-layer join between a rank's
 * {@link PlantFeatureAssignment}s and the {@link PlantFeature} entities they reference.
 * <p>
 * Composes with exactly two batched repository calls: one fetch of the assignments at
 * the given rank, one fetch of the referenced features by id set — never a per-assignment
 * feature fetch. No ancestry walk; that composition (lineage-composite, ancestor-first)
 * is a later slice, mirrored on {@code InsectFeatureQueryImpl.findByRankName}.
 */
@DomainService
class PlantFeatureQueryImpl implements PlantQuery.FeatureQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final PlantRepository.FeatureRepository featureRepository;
    private final PlantRepository.FeatureAssignmentRepository assignmentRepository;

    PlantFeatureQueryImpl(PlantRepository.FeatureRepository featureRepository,
                          PlantRepository.FeatureAssignmentRepository assignmentRepository) {
        observer.arguments("constructor", i -> i
                        .notNull(featureRepository, "featureRepository")
                        .notNull(assignmentRepository, "assignmentRepository"))
                .throwWhenInvalid();
        this.featureRepository = featureRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public FeatureCollection forRankName(PlantRankName rankName) {
        observer.arguments("forRankName", i -> i.identifier(rankName, "rankName")).throwWhenInvalid();
        List<PlantFeatureAssignment> assignments = assignmentRepository.getByRankName(rankName); // 1 batched call
        if (assignments.isEmpty()) return FeatureCollection.empty();
        Set<PlantFeatureId> ids = assignments.stream()
                .map(PlantFeatureAssignment::featureId)
                .collect(Collectors.toSet());
        List<PlantFeature> features = featureRepository.getByEntityNameSet(ids).stream().toList(); // 1 batched call
        return FeatureCollection.of(features);
    }
}
