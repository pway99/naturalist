package com.naturalist.plants;

import com.naturalist.Incubating;
import com.naturalist.data.NamedEntityRepository;

@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface PlantRepository {
    interface PlantEntityRepository extends NamedEntityRepository<PlantName, Plant> {}
}
