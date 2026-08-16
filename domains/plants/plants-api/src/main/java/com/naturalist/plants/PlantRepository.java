package com.naturalist.plants;

import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Optional;

class PlantRepository {
    protected interface PlantEntityRepository extends EntityRepository<PlantSpeciesName, PlantSpecies> {

        List<PlantSpecies> getByGenusName(PlantGenusName genusName);
    }

    protected interface PlantOrderEntityRepository
            extends EntityRepository<PlantOrderName, PlantOrder> {
    }

    protected interface PlantFamilyEntityRepository
            extends EntityRepository<PlantFamilyName, PlantFamily> {

        List<PlantFamily> getByOrderName(PlantOrderName orderName);
    }

    protected interface PlantGenusEntityRepository
            extends EntityRepository<PlantGenusName, PlantGenus> {

        List<PlantGenus> getByFamilyName(PlantFamilyName familyName);
    }

    protected interface PlantEcologicalRoleEntityRepository
            extends EntityRepository<PlantEcologicalRoleId, PlantEcologicalRole> {

        /** At most one role record per taxon — uniqueness is on {@code plantName}. */
        Optional<PlantEcologicalRole> getByPlantName(PlantRankName plantName);
    }
}
