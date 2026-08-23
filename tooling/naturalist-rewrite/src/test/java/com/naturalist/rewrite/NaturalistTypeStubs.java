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
}
