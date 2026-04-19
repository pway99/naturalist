package com.naturalist.data;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.PersistenceId;
import com.naturalist.ddd.EntityName;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public abstract class AbstractTestEntityRepository<ID extends PersistenceId<?>, NAME extends EntityName<?>, ENTITY extends Entity<ID, NAME>, TES extends TestEntitySource<ID, NAME, ENTITY>>
        implements EntityRepository<ID, NAME, ENTITY> {
    final NaturalistDatabase naturalistDatabase;
    final Class<TES> tes;

    protected AbstractTestEntityRepository(NaturalistDatabase naturalistDatabase) {
        this.naturalistDatabase = naturalistDatabase;
        this.tes = tesClass();
    }

    protected TestEntitySource<ID, NAME, ENTITY> testEntitySource() {
        return naturalistDatabase.get(tes);
    }

    @Override
    public Optional<ENTITY> doGetById(ID id) {
        return testEntitySource().get(id);
    }

    @Override
    public Optional<ENTITY> doGetByName(NAME name) {
        return testEntitySource().getByName(name);
    }

    @Override
    public void doInsert(ENTITY entity) {
        testEntitySource().insert(entity);
    }

    @Override
    public void doUpdate(ENTITY entity) {
        testEntitySource().update(entity);
    }

    @Override
    public List<ENTITY> doGetByIdSet(Set<ID> idSet) {
        return testEntitySource().entityStream()
                .filter(e -> idSet.contains(e.id()))
                .toList();
    }

    @Override
    public List<ENTITY> doGetByNameSet(Set<NAME> nameSet) {
        return testEntitySource().entityStream()
                .filter(e -> nameSet.contains(e.name()))
                .toList();
    }

    Class<TES> tesClass() {
        return (Class<TES>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[3];
    }
}
