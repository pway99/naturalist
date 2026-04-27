package com.naturalist.plants;

import com.naturalist.data.EntityRepository;

class PlantRepository {
    protected interface PlantEntityRepository extends EntityRepository<PlantName, Plant> {}
}
