package com.naturalist.data;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory repository adapter base for {@link Entity}. Delegates every hook to the
 * domain's {@link TestEntitySource} resolved from the shared {@link NaturalistDatabase}.
 * Inherits validation and the observer from {@link AbstractEntityRepository}.
 */
public abstract class AbstractTestEntityRepository<
        ID extends PersistenceId<?>,
        NAME extends EntityName<?>,
        ENTITY extends Entity<ID, NAME>,
        TES extends TestEntitySource<ID, NAME, ENTITY>>
        extends AbstractEntityRepository<ID, NAME, ENTITY> {

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
    protected Optional<ENTITY> doGetById(ID id) {
        return testEntitySource().get(id);
    }

    @Override
    protected Optional<ENTITY> doGetByName(NAME name) {
        return testEntitySource().getByName(name);
    }

    @Override
    protected void doInsert(ENTITY entity) {
        testEntitySource().insert(entity);
    }

    @Override
    protected void doUpdate(ENTITY entity) {
        testEntitySource().update(entity);
    }

    @Override
    protected List<ENTITY> doGetByIdSet(Set<ID> idSet) {
        return testEntitySource().entityStream()
                .filter(e -> idSet.contains(e.id()))
                .toList();
    }

    @Override
    protected List<ENTITY> doGetByNameSet(Set<NAME> nameSet) {
        return testEntitySource().entityStream()
                .filter(e -> nameSet.contains(e.name()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    Class<TES> tesClass() {
        return (Class<TES>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[3];
    }
}
