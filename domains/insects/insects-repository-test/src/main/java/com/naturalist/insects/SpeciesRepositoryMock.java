package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class SpeciesRepositoryMock
        extends AbstractTestEntityRepository<InsectSpeciesName, InsectSpecies, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {

    SpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectSpecies> getByFunctionalGuild(FunctionalGuild functionalGuild) {
        observer().arguments("getByFunctionalGuild", i -> i
                        .notNull(functionalGuild, "functionalGuild"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> s.guilds().contains(functionalGuild))
                .toList();
    }
}
