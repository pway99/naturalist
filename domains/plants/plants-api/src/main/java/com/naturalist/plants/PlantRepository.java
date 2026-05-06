package com.naturalist.plants;

import com.naturalist.data.EntityRepository;

import java.util.List;

class PlantRepository {
    protected interface PlantEntityRepository extends EntityRepository<PlantName, Plant> {
        List<PlantName> getAllPlantNames();
    }

    protected interface PlantFamilyEntityRepository
            extends EntityRepository<PlantFamilyName, PlantFamily> {
    }

    protected interface PlantGenusEntityRepository
            extends EntityRepository<PlantGenusName, PlantGenus> {
    }
}
