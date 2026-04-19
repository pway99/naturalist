package com.naturalist.insects;

import com.naturalist.Incubating;
import com.naturalist.data.EntityRepository;

@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface InsectSpeciesRepository {
    interface InsectSpeciesEntityRepository extends EntityRepository<InsectSpeciesId, InsectSpeciesName, InsectSpecies> {}
}
