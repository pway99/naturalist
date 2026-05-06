package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.ForeignKeyConstraintException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.exception.UniqueConstraintException;
import com.naturalist.observability.Observer;
import org.apache.commons.collections4.MapUtils;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Stream;

/**
 * In-memory data source for a {@link NamedEntity}. Keyed entirely on the entity's
 * {@link EntityName}; no numeric id is generated or tracked (ADR-021).
 *
 * <p>JSON fixtures for a {@code NamedEntity} have no id field. Insertion is
 * name-keyed; the primary-key collision check is a name collision check. Any
 * additional {@link UniqueConstraint}s declared by the subclass are enforced
 * alongside it.
 *
 * <p>Every subclass receives the surrounding {@link NaturalistDatabase} via
 * its constructor so that intra-domain referential-integrity checks (added
 * in subsequent work) can resolve foreign {@code TestEntitySource} peers
 * through {@link NaturalistDatabase#getNamed}. The reference is held even
 * for sources that declare no foreign-key constraints today — the wiring
 * is uniform.
 *
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the named entity type
 */
public abstract class TestEntitySource<NAME, ENTITY extends Named<NAME>> {

    private static final Observer observer = Observer.forClass(TestEntitySource.class);
    private final Map<NAME, ENTITY> entityMap = new HashMap<>();
    protected final NaturalistDatabase database;

    protected TestEntitySource(NaturalistDatabase database) {
        this.database = database;
    }

    protected List<UniqueConstraint<ENTITY>> uniqueConstraints() {
        return List.of();
    }

    protected List<ForeignKeyConstraint<ENTITY, ?>> foreignKeyConstraints() {
        return List.of();
    }

    public Stream<ENTITY> entityStream() {
        return entityMap.values().stream();
    }

    public Optional<ENTITY> getByName(NAME name) {
        return Optional.ofNullable(entityMap.get(name));
    }

    public List<ENTITY> getByEntityNameSet(Set<NAME> nameSet) {
        return entityMap.values().stream()
                .filter(e -> nameSet.contains(e.name()))
                .toList();
    }

    void preSaveChecks(ENTITY entity, NAME excludeName) {
        for (UniqueConstraint<ENTITY> uniqueConstraint : uniqueConstraints()) {
            Object entityValue = uniqueConstraint.value(entity);
            if (entityValue != null && entityMap.values().stream()
                    .filter(e -> excludeName == null || !e.name().equals(excludeName))
                    .map(uniqueConstraint.valueFunction())
                    .anyMatch(entityValue::equals)) {
                throw new UniqueConstraintException(entity, uniqueConstraint.name(), entityValue);
            }
        }
        for (ForeignKeyConstraint<ENTITY, ?> foreignKeyConstraint : foreignKeyConstraints()) {
            checkForeignKey(foreignKeyConstraint, entity);
        }
    }

    private <FK_NAME> void checkForeignKey(ForeignKeyConstraint<ENTITY, FK_NAME> fk, ENTITY entity) {
        FK_NAME foreignValue = fk.value(entity);
        if (foreignValue == null) {
            return;
        }
        TestEntitySource<FK_NAME, ?> foreignSource = database.getNamed(fk.foreignSourceClass());
        if (foreignSource.getByName(foreignValue).isEmpty()) {
            throw new ForeignKeyConstraintException(entity, fk.name(), foreignValue);
        }
    }

    public void insert(ENTITY entity) {
        final ENTITY argument = entity;
        observer.arguments("insert", i -> i.namedEntity(argument, "entity")).throwWhenInvalid();
        NAME name = entity.name();
        if (entityMap.containsKey(name)) {
            throw new PrimaryKeyConstraintException(entity);
        }
        preSaveChecks(entity, null);
        entityMap.put(name, entity);
    }

    public void update(ENTITY entity) {
        observer.arguments("update", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        NAME name = entity.name();
        if (name == null || !entityMap.containsKey(name)) {
            throw new EntityNotFoundException(entity);
        }
        preSaveChecks(entity, name);
        entityMap.replace(name, entity);
    }

    boolean isEmpty() {
        return MapUtils.isEmpty(entityMap);
    }

    public void loadFiles(String pathFormat, String... replacements) {
        Stream.of(replacements)
                .map(pathFormat::formatted)
                .forEach(this::loadFile);
    }

    public void loadFile(String relativePath) {
        String json = TestDataHelper.readFileToString(relativePath);
        List<ENTITY> entities = TestDataHelper.readObjectsFromString(() -> json, entityClass());
        for (ENTITY entity : entities) {
            insert(entity);
        }
    }

    @SuppressWarnings("unchecked")
    Class<ENTITY> entityClass() {
        return (Class<ENTITY>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }
}
