package com.naturalist.plants;

import com.naturalist.data.EntityRepository;

class PlantRepository {
    protected interface PlantEntityRepository extends EntityRepository<PlantName, Plant> {
    }

    protected interface PlantFamilyEntityRepository
            extends EntityRepository<PlantFamilyName, PlantFamily> {
    }

    protected interface PlantGenusEntityRepository
            extends EntityRepository<PlantGenusName, PlantGenus> {
    }
}
