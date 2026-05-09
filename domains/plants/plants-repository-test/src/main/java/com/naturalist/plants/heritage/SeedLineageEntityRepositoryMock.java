package com.naturalist.plants.heritage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.cultivar.CultivarName;

import java.util.List;

@DomainService
public class SeedLineageEntityRepositoryMock
        extends AbstractTestEntityRepository<SeedLineageName, SeedLineage, SeedLineageTestEntitySource>
        implements SeedLineageRepository.SeedLineageEntityRepository {

    protected SeedLineageEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<SeedLineage> getByCultivarName(CultivarName cultivarName) {
        return testEntitySource().entityStream()
                .filter(s -> s.cultivarName().equals(cultivarName))
                .toList();
    }
}
