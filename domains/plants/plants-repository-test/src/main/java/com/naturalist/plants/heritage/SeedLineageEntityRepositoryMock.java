package com.naturalist.plants.heritage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

public class SeedLineageEntityRepositoryMock
        extends AbstractTestEntityRepository<SeedLineageName, SeedLineage, SeedLineageTestEntitySource>
        implements SeedLineageRepository.SeedLineageEntityRepository {

    protected SeedLineageEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
