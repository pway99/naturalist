package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Set;

@MockDomainService
class InsectSpeciesRepositoryMock
        extends AbstractTestEntityRepository<InsectSpeciesName, InsectSpeciesInfo, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {

    InsectSpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectSpeciesInfo> getByGenusName(InsectGenusName genusName) {
        MethodObserver getByGenusName = observer().forMethod("getByGenusName");
        getByGenusName.arguments(i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(s -> genusName.equals(s.genusName()))
                .toList();
        getByGenusName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<InsectSpeciesInfo> getByGenusNames(Set<InsectGenusName> genusNames) {
        MethodObserver getByGenusNames = observer().forMethod("getByGenusNames");
        getByGenusNames.arguments(i -> i.entityNameCollection(genusNames, "genusNames"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(s -> genusNames.contains(s.genusName()))
                .toList();
        getByGenusNames.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
