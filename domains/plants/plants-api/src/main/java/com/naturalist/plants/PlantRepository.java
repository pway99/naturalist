package com.naturalist.plants;

import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Optional;

class PlantRepository {
    protected interface PlantEntityRepository extends EntityRepository<PlantSpeciesName, PlantSpecies> {
    }

    protected interface PlantFamilyEntityRepository
            extends EntityRepository<PlantFamilyName, PlantFamily> {
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
