package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class NoDirectTestEntitySourceConstructionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoDirectTestEntitySourceConstruction())
            // NaturalistTypeStubs are in-memory parsed sources, never compiled to real .class
            // files, so JavaTemplate's snippet compiler (which needs an actual classpath entry)
            // cannot fully attribute the `<X>TestEntitySource.class` literal or the generic
            // `getNamed(Class<T>)` invocation it builds. The recipe's real-world target sources
            // are always fully compiled, where this resolves correctly; see
            // https://docs.openrewrite.org/reference/faq#im-seeing-lst-contains-missing-or-invalid-type-information-in-my-recipe-unit-tests-how-to-resolve
            .typeValidationOptions(TypeValidation.builder()
                .identifiers(false)
                .methodInvocations(false)
                .build());
    }

    @Test
    void rewritesNewToGetNamed() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.FOO_SOURCE),
            java(
                """
                import com.naturalist.data.FooTestEntitySource;
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    void m(NaturalistDatabase db) {
                        FooTestEntitySource s = new FooTestEntitySource(db);
                    }
                }
                """,
                """
                import com.naturalist.data.FooTestEntitySource;
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    void m(NaturalistDatabase db) {
                        FooTestEntitySource s = db.getNamed(FooTestEntitySource.class);
                    }
                }
                """
            )
        );
    }

    @Test
    void leavesTheAbstractBaseSuperCallAlone() {
        // FooTestEntitySource's own `super(database)` is a method invocation, not a NewClass;
        // parsing the stub as-is must produce no change.
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.FOO_SOURCE)   // no `after` => asserts unchanged
        );
    }

    @Test
    void markSourceConstructedWithUnexpectedArity() {
        // BarTestEntitySource's constructor takes 2 arguments; the recipe must not attempt a
        // rewrite it can't safely express (marker-not-corrupt floor) and instead flags it.
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.BAR_SOURCE),
            java(
                """
                import com.naturalist.data.BarTestEntitySource;
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    void m(NaturalistDatabase db) {
                        BarTestEntitySource s = new BarTestEntitySource(db, "x");
                    }
                }
                """,
                """
                import com.naturalist.data.BarTestEntitySource;
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    void m(NaturalistDatabase db) {
                        BarTestEntitySource s = /*~~(unexpected TestEntitySource constructor arity; acquire via db.getNamed(...))~~>*/new BarTestEntitySource(db, "x");
                    }
                }
                """
            )
        );
    }
}
