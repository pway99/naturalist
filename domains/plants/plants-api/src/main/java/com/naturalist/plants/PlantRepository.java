package com.naturalist.plants;

import com.naturalist.data.EntityRepository;

import java.util.List;

class PlantRepository {
    protected interface PlantEntityRepository extends EntityRepository<PlantName, Plant> {
        List<PlantName> getAllPlantNames();
    }

    protected interface PlantFamilyEntityRepository
            extends EntityRepository<PlantFamilyName, PlantFamily> {

        List<PlantFamilyName> getAllFamilyNames();
    }

    protected interface PlantGenusEntityRepository
            extends EntityRepository<PlantGenusName, PlantGenus> {

        List<PlantGenusName> getAllGenusNames();
    }
}
