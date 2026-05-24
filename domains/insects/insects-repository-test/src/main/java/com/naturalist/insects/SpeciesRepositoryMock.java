package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.taxonomy.TaxonomicGenus;

import java.util.List;

@DomainService
class SpeciesRepositoryMock
        extends AbstractTestEntityRepository<InsectSpeciesName, InsectSpecies, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {

    SpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectSpecies> getByGenusEpithet(TaxonomicGenus genusEpithet) {
        observer().arguments("getByGenusEpithet",
                        i -> i.namedValue(genusEpithet, "genusEpithet"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> s.belongsToGenus(genusEpithet))
                .toList();
    }
}
