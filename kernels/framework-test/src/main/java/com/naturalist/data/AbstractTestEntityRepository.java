package com.naturalist.data;

import com.naturalist.ddd.Named;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory repository adapter base for a {@link Named} entity. Delegates every hook
 * to the domain's {@link TestEntitySource} resolved from the shared
 * {@link NaturalistDatabase}. Inherits validation and the observer from
 * {@link AbstractEntityRepository}.
 *
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the named entity type
 * @param <NTS>    the concrete {@link TestEntitySource} type backing this repository
 */
public abstract class AbstractTestEntityRepository<
        NAME,
        ENTITY extends Named<NAME>,
        NTS extends TestEntitySource<NAME, ENTITY>>
        extends AbstractEntityRepository<NAME, ENTITY> {

    final NaturalistDatabase naturalistDatabase;
    final Class<NTS> nts;

    protected AbstractTestEntityRepository(NaturalistDatabase naturalistDatabase) {
        this.naturalistDatabase = naturalistDatabase;
        this.nts = ntsClass();
    }

    protected TestEntitySource<NAME, ENTITY> testEntitySource() {
        return naturalistDatabase.getNamed(nts);
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
    protected List<ENTITY> doGetByNameSet(Set<NAME> nameSet) {
        return testEntitySource().entityStream()
                .filter(e -> nameSet.contains(e.name()))
                .toList();
    }

    @Override
    protected Page<ENTITY> doGetPage(PageRequest pageRequest) {
        return testEntitySource().pageOf(pageRequest);
    }

    @SuppressWarnings("unchecked")
    Class<NTS> ntsClass() {
        return (Class<NTS>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[2];
    }
}
