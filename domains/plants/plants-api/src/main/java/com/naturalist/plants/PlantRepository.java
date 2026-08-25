package com.naturalist.plants;

import com.naturalist.data.EntityRepository;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.taxonomy.OrganismFeatureAssignment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class PlantRepository {
    protected interface SpeciesRepository extends EntityRepository<PlantSpeciesName, PlantSpecies> {

        List<PlantSpecies> getByGenusName(PlantGenusName genusName);

        /** Batched sibling of {@link #getByGenusName} across a set of genera. */
        List<PlantSpecies> getByGenusNames(Set<PlantGenusName> genusNames);
    }

    protected interface OrderRepository
            extends EntityRepository<PlantOrderName, PlantOrder> {
    }

    protected interface FamilyRepository
            extends EntityRepository<PlantFamilyName, PlantFamily> {

        List<PlantFamily> getByOrderName(PlantOrderName orderName);
    }

    protected interface GenusRepository
            extends EntityRepository<PlantGenusName, PlantGenus> {

        List<PlantGenus> getByFamilyName(PlantFamilyName familyName);
    }

    protected interface EcologicalRoleRepository
            extends EntityRepository<PlantEcologicalRoleId, PlantEcologicalRole> {

        /** At most one role record per taxon — uniqueness is on {@code plantName}. */
        Optional<PlantEcologicalRole> getByPlantName(PlantRankName plantName);
    }

    protected interface ImageRepository
            extends EntityRepository<PlantImageId, OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> {

        List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> getByParentName(PlantRankName parentName);
    }

    protected interface ObservationRepository
            extends EntityRepository<PlantObservationId, OrganismObservation<PlantObservationId, PlantRankName>> {

        List<OrganismObservation<PlantObservationId, PlantRankName>> getByNaturalist(NaturalistName observedBy);

        List<OrganismObservation<PlantObservationId, PlantRankName>> getByNaturalistAndSubjects(
                NaturalistName observedBy, Set<PlantRankName> subjects);
    }

    protected interface FeatureRepository
            extends EntityRepository<PlantFeatureId, PlantFeature> {
    }

    protected interface FeatureAssignmentRepository
            extends EntityRepository<PlantFeatureAssignmentId,
                                     OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> {

        List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> getByRankName(PlantRankName rankName);

        List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> getByRankNames(Set<PlantRankName> rankNames);

        List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> getByFeatureId(PlantFeatureId featureId);
    }
}
