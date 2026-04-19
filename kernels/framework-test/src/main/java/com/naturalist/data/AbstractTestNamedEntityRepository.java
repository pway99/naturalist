package com.naturalist.data;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedEntity;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory repository adapter base for a {@link NamedEntity}. Delegates every hook
 * to the domain's {@link NamedTestEntitySource} resolved from the shared
 * {@link NaturalistDatabase}. Inherits validation and the observer from
 * {@link AbstractNamedEntityRepository}.
 *
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the named entity type
 * @param <NTS>    the concrete {@link NamedTestEntitySource} type backing this repository
 */
public abstract class AbstractTestNamedEntityRepository<
        NAME extends EntityName<?>,
        ENTITY extends NamedEntity<NAME>,
        NTS extends NamedTestEntitySource<NAME, ENTITY>>
        extends AbstractNamedEntityRepository<NAME, ENTITY> {

    final NaturalistDatabase naturalistDatabase;
    final Class<NTS> nts;

    protected AbstractTestNamedEntityRepository(NaturalistDatabase naturalistDatabase) {
        this.naturalistDatabase = naturalistDatabase;
        this.nts = ntsClass();
    }

    protected NamedTestEntitySource<NAME, ENTITY> testEntitySource() {
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

    @SuppressWarnings("unchecked")
    Class<NTS> ntsClass() {
        return (Class<NTS>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[2];
    }
}
