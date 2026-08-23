package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class NoCachedTestEntitySourceFieldTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoCachedTestEntitySourceField());
    }

    @Test
    void flagsACachedSourceField() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.FOO_SOURCE),
            srcTestJava(
                java(
                    """
                    import com.naturalist.data.FooTestEntitySource;
                    import com.naturalist.data.NaturalistDatabase;
                    class T {
                        private final FooTestEntitySource src = new NaturalistDatabase().getNamed(FooTestEntitySource.class);
                    }
                    """,
                    """
                    import com.naturalist.data.FooTestEntitySource;
                    import com.naturalist.data.NaturalistDatabase;
                    class T {
                        /*~~(cache TestEntitySource in a field; fetch via db.getNamed(...) inside each test so sources reset per test)~~>*/private final FooTestEntitySource src = new NaturalistDatabase().getNamed(FooTestEntitySource.class);
                    }
                    """
                )
            )
        );
    }

    @Test
    void doesNotFlagMethodLocalOrHelper() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.FOO_SOURCE),
            srcTestJava(
                java(
                    // local var + helper method returning getNamed — both allowed, no change.
                    """
                    import com.naturalist.data.FooTestEntitySource;
                    import com.naturalist.data.NaturalistDatabase;
                    class T {
                        private final NaturalistDatabase db = new NaturalistDatabase();
                        private FooTestEntitySource source() { return db.getNamed(FooTestEntitySource.class); }
                        void m() {
                            FooTestEntitySource local = db.getNamed(FooTestEntitySource.class);
                        }
                    }
                    """
                )
            )
        );
    }

    @Test
    void mainSourceCachedFieldIsNotFlagged() {
        // R3 is test-source-scoped: the same cached-field shape that gets flagged in src/test
        // (see flagsACachedSourceField) must be left untouched when it lives in src/main — e.g.
        // the main-source contract base TestEntitySourceTest (ADR-001) intentionally holds
        // TestEntitySource-typed state and this recipe must never touch it.
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.FOO_SOURCE),
            srcMainJava(
                java(
                    """
                    import com.naturalist.data.FooTestEntitySource;
                    import com.naturalist.data.NaturalistDatabase;
                    class T {
                        private final FooTestEntitySource src = new NaturalistDatabase().getNamed(FooTestEntitySource.class);
                    }
                    """   // no `after` => unchanged in main source
                )
            )
        );
    }
}
