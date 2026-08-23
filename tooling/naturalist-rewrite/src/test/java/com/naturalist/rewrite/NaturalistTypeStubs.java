package com.naturalist.rewrite;

/** Minimal source stubs for the naturalist data types the recipes match against.
 *  Passed as unchanged supporting sources in RewriteTest so type attribution resolves. */
final class NaturalistTypeStubs {
    private NaturalistTypeStubs() {}

    static final String NATURALIST_DATABASE = """
        package com.naturalist.data;
        public class NaturalistDatabase {
            public static NaturalistDatabase create() { return new NaturalistDatabase(); }
            public <T> T getNamed(Class<T> sourceClass) { return null; }
        }
        """;

    static final String TEST_ENTITY_SOURCE = """
        package com.naturalist.data;
        public abstract class TestEntitySource {
            protected TestEntitySource(NaturalistDatabase database) {}
        }
        """;

    static final String NATURALIST_TEST_EXTENSION = """
        package com.naturalist.data;
        public class NaturalistTestExtension extends NaturalistDatabase {
            public static NaturalistTestExtension create() { return new NaturalistTestExtension(); }
        }
        """;

    static final String FOO_SOURCE = """
        package com.naturalist.data;
        public final class FooTestEntitySource extends TestEntitySource {
            public FooTestEntitySource(NaturalistDatabase database) { super(database); }
        }
        """;

    static final String BAR_SOURCE = """
        package com.naturalist.data;
        public final class BarTestEntitySource extends TestEntitySource {
            public BarTestEntitySource(NaturalistDatabase database, String label) { super(database); }
        }
        """;

    static final String ENTITY_REPOSITORY = """
        package com.naturalist.data;
        import java.util.List;
        import java.util.Optional;
        import java.util.Set;
        public interface EntityRepository<NAME, ENTITY> {
            Optional<ENTITY> getByName(NAME name);
            List<ENTITY> getByEntityNameSet(Set<NAME> nameSet);
            void insert(ENTITY entity);
            void update(ENTITY entity);
            ENTITY save(ENTITY entity);
        }
        """;

    static final String ENTITY_QUERY = """
        package com.naturalist.data;
        import java.util.List;
        import java.util.Optional;
        import java.util.Set;
        public interface EntityQuery<NAME, E> {
            Optional<E> getByName(NAME name);
            List<E> findByNameSet(Set<NAME> nameSet);
        }
        """;

    static final String FOO_REPOSITORY = """
        package com.naturalist.data;
        import java.util.List;
        import java.util.Optional;
        import java.util.Set;
        public interface FooRepository extends EntityRepository<String, String> {
            List<String> getByParentNames(Set<String> parents);
        }
        """;

    static final String FOO_QUERY = """
        package com.naturalist.data;
        import java.util.List;
        import java.util.Optional;
        import java.util.Set;
        public interface FooQuery extends EntityQuery<String, String> {
        }
        """;
}
