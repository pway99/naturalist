package com.naturalist.plants;

import com.naturalist.Incubating;
import com.naturalist.data.EntityRepository;

@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface PlantRepository {
    interface PlantEntityRepository extends EntityRepository<PlantName, Plant> {}
}
