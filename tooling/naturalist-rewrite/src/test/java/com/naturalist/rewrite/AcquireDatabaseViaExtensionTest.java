package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class AcquireDatabaseViaExtensionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AcquireDatabaseViaExtension())
            // Put a REAL, resolvable org.junit.jupiter.api.extension.RegisterExtension on the parse
            // classpath (junit-jupiter-api is already a transitive test dependency of this module via
            // the junit-jupiter aggregator, so it's on this process's runtime classpath for
            // JavaParser's classpath(String...) scan to find). Against these NaturalistTypeStubs
            // alone, RegisterExtension would stay an unresolved bare identifier and never exercise
            // the "annotation + declaration parses as 2 statements" failure mode that only appears
            // once the annotation type genuinely resolves (as it does in a real, fully-compiled
            // module) — this is exactly the gap that let the original monolithic-template
            // implementation ship a crash undetected by these unit tests.
            .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
            // NaturalistTypeStubs are parsed sources, never compiled to real .class files, so the
            // JavaTemplate-generated NaturalistTestExtension identifiers can't be fully
            // type-attributed here (unlike RegisterExtension above, which now resolves for real).
            // The recipe's real-world target sources are always fully compiled, where this resolves
            // correctly; see
            // https://docs.openrewrite.org/reference/faq#im-seeing-lst-contains-missing-or-invalid-type-information-in-my-recipe-unit-tests-how-to-resolve
            .typeValidationOptions(TypeValidation.builder()
                .identifiers(false)
                .build());
    }

    @Test
    void caseA_fieldInitializerIsRetypedToTheExtension() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.NATURALIST_TEST_EXTENSION),
            srcTestJava(
                java(
                    """
                    import com.naturalist.data.NaturalistDatabase;
                    class T {
                        private final NaturalistDatabase db = NaturalistDatabase.create();
                    }
                    """,
                    """
                    import com.naturalist.data.NaturalistTestExtension;
                    import org.junit.jupiter.api.extension.RegisterExtension;

                    class T {
                        @RegisterExtension
                        private final NaturalistTestExtension db = NaturalistTestExtension.create();
                    }
                    """
                )
            )
        );
    }

    @Test
    void methodLocalIsMarkedNotRewritten() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.NATURALIST_TEST_EXTENSION),
            srcTestJava(
                java(
                    """
                    import com.naturalist.data.NaturalistDatabase;
                    class T {
                        void m() {
                            NaturalistDatabase db = NaturalistDatabase.create();
                        }
                    }
                    """,
                    """
                    import com.naturalist.data.NaturalistDatabase;
                    class T {
                        void m() {
                            NaturalistDatabase db = /*~~(hoist a @RegisterExtension NaturalistTestExtension field; do not create a bare NaturalistDatabase in a test)~~>*/NaturalistDatabase.create();
                        }
                    }
                    """
                )
            )
        );
    }

    @Test
    void extensionCreateIsNotFlagged() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.NATURALIST_TEST_EXTENSION),
            srcTestJava(
                java(
                    """
                    import com.naturalist.data.NaturalistTestExtension;
                    class T {
                        private final NaturalistTestExtension db = NaturalistTestExtension.create();
                    }
                    """   // no `after` => unchanged
                )
            )
        );
    }

    @Test
    void mainSourceFieldInitializerIsNotRewritten() {
        // R2 is test-source-scoped: the same Case-A shape that gets retyped in src/test
        // (see caseA_fieldInitializerIsRetypedToTheExtension) must be left untouched when it
        // lives in src/main — e.g. TestDataConfiguration's Spring @Bean `return
        // NaturalistDatabase.create();` and the main-source contract base TestEntitySourceTest
        // (ADR-001) are sanctioned create() callers this recipe must never touch.
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.NATURALIST_TEST_EXTENSION),
            srcMainJava(
                java(
                    """
                    import com.naturalist.data.NaturalistDatabase;
                    class T {
                        private final NaturalistDatabase db = NaturalistDatabase.create();
                    }
                    """   // no `after` => unchanged in main source
                )
            )
        );
    }
}
