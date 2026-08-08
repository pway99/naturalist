package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.ForeignKeyConstraintException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.exception.UniqueConstraintException;
import com.naturalist.observability.Observer;
import org.apache.commons.collections4.MapUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    /**
     * Whether {@code insert}/{@code update} flush state back to the source-tree
     * JSON file the entity came from. Read once at class load. Tests do not
     * set the system property → field is {@code false} for the test JVM's
     * lifetime, no {@code @BeforeEach} or runtime {@code System.setProperty}
     * can flip it. Production startup ({@code ConsoleApplication.main}) sets
     * the property before {@code SpringApplication.run}, so this field is
     * already {@code true} when the first {@code TestEntitySource} subclass
     * loads.
     */
    private static final boolean PERSISTENCE_ENABLED =
            Boolean.getBoolean("naturalist.persistence.enabled");

    private static final Observer observer = Observer.forClass(TestEntitySource.class);
    private final Map<NAME, ENTITY> entityMap = new HashMap<>();
    private final Map<NAME, String> originFile = new HashMap<>();
    private @Nullable String defaultInsertFile;
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
                .filter(e -> nameSet.contains(e.key()))
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
                .sorted(Comparator.comparing(e -> e.key().toString()))
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
                    .filter(e -> excludeName == null || !e.key().equals(excludeName))
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
        insertCommon(entity);
        NAME name = entity.key();
        if (!originFile.containsKey(name)) {
            originFile.put(name, defaultInsertFile);
        }
        flushIfWritable();
    }

    private void insertCommon(ENTITY entity) {
        final ENTITY argument = entity;
        observer.arguments("insert", i -> i.namedEntity(argument, "entity")).throwWhenInvalid();
        NAME name = entity.key();
        if (entityMap.containsKey(name)) {
            throw new PrimaryKeyConstraintException(entity);
        }
        preSaveChecks(entity, null);
        entityMap.put(name, entity);
    }

    public void update(ENTITY entity) {
        observer.arguments("update", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        NAME name = entity.key();
        if (name == null || !entityMap.containsKey(name)) {
            throw new EntityNotFoundException(entity);
        }
        preSaveChecks(entity, name);
        entityMap.replace(name, entity);
        flushIfWritable();
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
        loadFile(relativePath,
                json -> TestDataHelper.readObjectsFromString(() -> json, entityClass()));
    }

    /**
     * Loads a catalog file whose on-disk shape is not the entity's own Jackson
     * shape — the parser turns the file's text into entities. Origin tracking and
     * {@code defaultInsertFile} registration behave exactly as for
     * {@link #loadFile(String)}, so entities inserted later (e.g. by a console
     * write) flush back to this file rather than being silently dropped.
     *
     * <p>A source using this overload MUST also override {@link #writable} and
     * {@link #writableClass()} so the flush writes the same shape the parser
     * reads. Overriding one without the other produces a file the loader cannot
     * read back on the next boot.
     */
    protected void loadFile(String relativePath, Function<String, List<ENTITY>> parser) {
        if (defaultInsertFile == null) {
            defaultInsertFile = relativePath;
        }
        String json = TestDataHelper.readFileToString(relativePath);
        for (ENTITY entity : parser.apply(json)) {
            insertCommon(entity);
            originFile.put(entity.key(), relativePath);
        }
    }

    /**
     * The on-disk form of a single entity. Defaults to the entity itself.
     * Override together with {@link #writableClass()} when the catalog file's
     * shape differs from the entity's Jackson shape — for instance when a
     * component is an open interface or abstract type Jackson cannot
     * round-trip.
     */
    protected Object writable(ENTITY entity) {
        return entity;
    }

    /** The declared type {@link #writable} returns. */
    protected Class<?> writableClass() {
        return entityClass();
    }

    /**
     * Group entities by the file they came from and rewrite each file in place. Only
     * fires when persistence is enabled (production composition root). Per-flush:
     * entries with no resolvable source-tree path (e.g. running outside a Maven
     * layout) are skipped silently — the heuristic matches the project's dev-tool
     * framing rather than failing.
     */
    private void flushIfWritable() {
        if (!PERSISTENCE_ENABLED) return;
        // Sort by name().toString() to match pageOf's read-side ordering. The entity
        // map is a HashMap, so iteration order is unstable; without a sort, every flush
        // reshuffles file contents and produces noisy diffs.
        Map<String, List<ENTITY>> byFile = entityMap.values().stream()
                .filter(e -> originFile.get(e.key()) != null)
                .sorted(Comparator.comparing(e -> e.key().toString()))
                .collect(Collectors.groupingBy(e -> originFile.get(e.key())));
        for (Map.Entry<String, List<ENTITY>> entry : byFile.entrySet()) {
            Path target = resolveSourcePath(entry.getKey());
            if (target == null) continue;
            writeJsonAtomic(target, entry.getValue());
        }
    }

    private @Nullable Path resolveSourcePath(String relativePath) {
        URL classpathUrl = getClass().getResource("/" + relativePath);
        if (classpathUrl == null) return null;
        try {
            String filePath = Paths.get(classpathUrl.toURI()).toString();
            if (!filePath.contains("/target/classes/")) return null;
            return Paths.get(filePath.replace("/target/classes/", "/src/main/resources/"));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private void writeJsonAtomic(Path target, List<ENTITY> entities) {
        try {
            var listType = TestDataHelper.mapper.getTypeFactory()
                    .constructCollectionType(List.class, writableClass());
            List<Object> writables = entities.stream()
                    .map(this::writable)
                    .collect(Collectors.toList());
            byte[] bytes = TestDataHelper.mapper.writerFor(listType)
                    .withDefaultPrettyPrinter()
                    .writeValueAsBytes(writables);
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(tmp, bytes);
            Files.move(tmp, target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    Class<ENTITY> entityClass() {
        return (Class<ENTITY>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }
}
