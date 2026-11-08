package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Set;

@MockDomainService
class InsectFamilyRepositoryMock
        extends AbstractTestEntityRepository<InsectFamilyName, InsectFamilyInfo, InsectFamilyTestEntitySource>
        implements InsectRepository.FamilyRepository {

    InsectFamilyRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectFamilyInfo> getByOrderName(InsectOrderName orderName) {
        MethodObserver getByOrderName = observer().forMethod("getByOrderName");
        getByOrderName.arguments(i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(f -> orderName.equals(f.orderName()))
                .toList();
        getByOrderName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<InsectFamilyInfo> getByOrderNames(Set<InsectOrderName> orderNames) {
        MethodObserver getByOrderNames = observer().forMethod("getByOrderNames");
        getByOrderNames.arguments(i -> i.entityNameCollection(orderNames, "orderNames"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(f -> orderNames.contains(f.orderName()))
                .toList();
        getByOrderNames.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
