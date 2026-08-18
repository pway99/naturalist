package com.naturalist.plants;

import com.naturalist.data.EntityRepository;
import com.naturalist.naturalist.NaturalistName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class PlantRepository {
    protected interface SpeciesRepository extends EntityRepository<PlantSpeciesName, PlantSpecies> {

        List<PlantSpecies> getByGenusName(PlantGenusName genusName);
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
            extends EntityRepository<PlantImageId, PlantImage> {

        List<PlantImage> getByParentName(PlantRankName parentName);
    }

    protected interface FieldObservationRepository
            extends EntityRepository<FieldObservationId, FieldObservation> {

        List<FieldObservation> getByNaturalist(NaturalistName observedBy);

        List<FieldObservation> getByNaturalistAndSubjects(
                NaturalistName observedBy, Set<PlantRankName> subjects);
    }
}
