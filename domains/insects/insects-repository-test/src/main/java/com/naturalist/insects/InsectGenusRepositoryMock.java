package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Set;

@MockDomainService
class InsectGenusRepositoryMock
        extends AbstractTestEntityRepository<InsectGenusName, InsectGenusInfo, InsectGenusTestEntitySource>
        implements InsectRepository.GenusRepository {

    InsectGenusRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectGenusInfo> getByFamilyName(InsectFamilyName familyName) {
        MethodObserver getByFamilyName = observer().forMethod("getByFamilyName");
        getByFamilyName.arguments(i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(g -> familyName.equals(g.familyName()))
                .toList();
        getByFamilyName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<InsectGenusInfo> getByFamilyNames(Set<InsectFamilyName> familyNames) {
        MethodObserver getByFamilyNames = observer().forMethod("getByFamilyNames");
        getByFamilyNames.arguments(i -> i.entityNameCollection(familyNames, "familyNames"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(g -> familyNames.contains(g.familyName()))
                .toList();
        getByFamilyNames.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
