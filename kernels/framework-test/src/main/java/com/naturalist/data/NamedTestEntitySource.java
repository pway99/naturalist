package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.exception.UniqueConstraintException;
import com.naturalist.observability.Observer;
import org.apache.commons.collections4.MapUtils;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the named entity type
 */
public abstract class NamedTestEntitySource<NAME, ENTITY extends Named<NAME>> {

    private static final Observer observer = Observer.forClass(NamedTestEntitySource.class);
    private final Map<NAME, ENTITY> entityMap = new HashMap<>();

    protected List<UniqueConstraint<ENTITY>> uniqueConstraints() {
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
