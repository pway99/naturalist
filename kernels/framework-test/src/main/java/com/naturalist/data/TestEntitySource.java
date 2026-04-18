package com.naturalist.data;

import com.naturalist.ddd.*;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.exception.UniqueConstraintException;
import com.naturalist.observability.Observer;
import org.apache.commons.collections4.MapUtils;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * In-memory data source for {@link CatalogEntity} — extends {@link TestEntitySource}
 * with name-based lookup operations.
 * <p>
 * Catalog entities carry a stable {@link CatalogName} slug. This source provides
 * {@code getByName} and {@code getByEntityNameSet} for resolving slug references
 * into full entity instances — the in-memory analog of a natural-key index.
 */
public abstract class TestEntitySource<ID extends PersistenceId<?>, NAME extends EntityName<?>, ENTITY extends Entity<ID, NAME>> {
    private static final Observer observer = Observer.forClass(TestEntitySource.class);
    private final Map<ID, ENTITY> entityMap = new HashMap<>();
    private final Function<Long, ID> idFactory;

    protected TestEntitySource(Function<Long, ID> idFactory) {
        this.idFactory = idFactory;
    }

    protected List<UniqueConstraint<ENTITY>> uniqueConstraints() {
        return List.of();
    }

    public Stream<ENTITY> entityStream() {
        return entityMap.values().stream();
    }

    public Optional<ENTITY> get(ID id) {
        return Optional.ofNullable(entityMap.get(id));
    }
    public Optional<ENTITY> getByName(NAME name) {
        return entityMap.values().stream()
                .filter(e -> e.name().equals(name))
                .findFirst();
    }
    public List<ENTITY> getByEntityNameSet(Set<NAME> nameSet) {
        return entityMap.values().stream()
                .filter(e -> nameSet.contains(e.name()))
                .toList();
    }

    void preSaveChecks(ENTITY entity, ID excludeId) {
        NAME name = entity.name();
        if (name != null && entityMap.values().stream()
                .filter(e -> excludeId == null || !e.id().equals(excludeId))
                .anyMatch(e -> e.name().equals(name))) {
            throw new UniqueConstraintException(entity, "name", name);
        }
        for (UniqueConstraint<ENTITY> uniqueConstraint : uniqueConstraints()) {
            Object entityValue = uniqueConstraint.value(entity);
            if (entityValue != null && entityMap.values().stream()
                    .filter(e -> excludeId == null || !e.id().equals(excludeId))
                    .map(uniqueConstraint.valueFunction())
                    .anyMatch(entityValue::equals)) {
                throw new UniqueConstraintException(entity, uniqueConstraint.name(), entityValue);
            }
        }
    }

    public void insert(ENTITY entity) {
        final ENTITY argument = entity;
        observer.arguments("insert", i -> i.entity(argument, "entity")).throwWhenInvalid();
        ID id = entity.id();
        if (id == null) {
            entity = entity.withId(idFactory.apply(nextNumericId()));
        }
        if (get(entity.id()).isPresent()) {
            throw new PrimaryKeyConstraintException(entity);
        }
        preSaveChecks(entity, null);
        entityMap.put(entity.id(), entity);
    }

    public void update(ENTITY entity) {
        observer.arguments("update", i -> i.entity(entity, "entity")).throwWhenInvalid();
        ID id = entity.id();
        if (id == null || id.isNotValid() || get(id).isEmpty()) {
            throw new EntityNotFoundException(entity);
        }
        preSaveChecks(entity, id);
        entityMap.replace(id, entity);
    }

    public Optional<ENTITY> getById(ID id) {
        return Optional.ofNullable(entityMap.get(id));
    }

    private long nextNumericId() {
        long maxId = 0L;
        for (ID id : entityMap.keySet()) {
            if (id != null) {
                if (id.value() instanceof Long l) {
                    if (l > maxId) maxId = l;
                } else {
                    throw new RuntimeException("Non numeric ids not supported");
                }
            }
        }
        return maxId + 1;
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

    Class<ENTITY> entityClass() {
        return (Class<ENTITY>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[2];
    }

}
