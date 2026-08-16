package com.naturalist.plants;

import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Optional;

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
}
