package com.naturalist.plants.heritage;

import com.naturalist.data.EntityRepository;
import com.naturalist.plants.cultivar.CultivarName;

import java.util.List;

class SeedLineageRepository {
    protected interface SeedLineageEntityRepository
            extends EntityRepository<SeedLineageName, SeedLineage> {

        List<SeedLineage> getByCultivarName(CultivarName cultivarName);
    }
}
