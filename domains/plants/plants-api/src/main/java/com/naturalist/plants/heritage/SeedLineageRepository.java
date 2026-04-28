package com.naturalist.plants.heritage;

import com.naturalist.data.EntityRepository;

class SeedLineageRepository {
    protected interface SeedLineageEntityRepository
            extends EntityRepository<SeedLineageName, SeedLineage> {}
}
