package com.naturalist.plants.heritage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.cultivar.CultivarName;

import java.util.List;

@DomainService
class SeedLineageEntityRepositoryMock
        extends AbstractTestEntityRepository<SeedLineageName, SeedLineage, SeedLineageTestEntitySource>
        implements SeedLineageRepository.SeedLineageEntityRepository {

    SeedLineageEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<SeedLineage> getByCultivarName(CultivarName cultivarName) {
        observer().arguments("getByCultivarName",
                        i -> i.entityName(cultivarName, "cultivarName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> cultivarName.equals(s.cultivarName()))
                .toList();
    }
}
