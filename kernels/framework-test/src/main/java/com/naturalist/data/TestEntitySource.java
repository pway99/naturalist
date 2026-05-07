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

    /**
     * Read a page of entities ordered ascending by {@code name().toString()}. The
     * in-memory adapter sorts on read — the entity map is a {@link HashMap} whose
     * iteration order is unstable, so a deterministic comparator is applied per
     * call. Cost is negligible at fixture scale; a production rdbms adapter would
     * delegate the ordering to {@code ORDER BY} on an indexed column.
     *
     * <p>Lookahead is honoured directly off the sorted list: when
     * {@link PageRequest#lookahead()} is positive, {@code pagesAheadKnown} is the
     * ceiling division of the remaining-row count by {@code pageSize}, capped at
     * {@code lookahead}; {@code moreBeyondLookahead} is set when at least one row
     * exists past the lookahead window. When lookahead is zero, only the cheap
     * "more exists?" signal is populated. The contract observed here matches what
     * the rdbms adapter must produce via its {@code LIMIT pageSize+1} page query
     * plus optional thin probe (see {@code docs/plans/paged-queries-plan.md}
     * Section 2).
     */
    public Page<ENTITY> pageOf(PageRequest pageRequest) {
        List<ENTITY> sorted = entityMap.values().stream()
                .sorted(Comparator.comparing(e -> e.name().toString()))
                .toList();
        int total = sorted.size();
        int offset = pageRequest.offset();
        int pageSize = pageRequest.pageSize();
        int lookahead = pageRequest.lookahead();

        if (offset >= total) {
            return new Page<>(List.of(), pageRequest.pageNumber(), pageSize, 0, false);
        }
        int end = Math.min(offset + pageSize, total);
        List<ENTITY> content = sorted.subList(offset, end);
        int remaining = total - end;

        int pagesAheadKnown;
        boolean moreBeyondLookahead;
        if (lookahead == 0) {
            pagesAheadKnown = 0;
            moreBeyondLookahead = remaining > 0;
        } else {
            int pagesNeededForRemaining = (remaining + pageSize - 1) / pageSize;
            pagesAheadKnown = Math.min(pagesNeededForRemaining, lookahead);
            moreBeyondLookahead = remaining > lookahead * pageSize;
        }
        return new Page<>(content, pageRequest.pageNumber(), pageSize, pagesAheadKnown, moreBeyondLookahead);
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
