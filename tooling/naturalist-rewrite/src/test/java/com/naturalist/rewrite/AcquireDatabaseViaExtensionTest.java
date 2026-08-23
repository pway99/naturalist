package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class AcquireDatabaseViaExtensionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AcquireDatabaseViaExtension())
            // Same in-memory-stub type-attribution gap as Task 2's recipe test: NaturalistTypeStubs
            // are parsed sources, never compiled to real .class files, so the JavaTemplate-generated
            // NaturalistTestExtension/RegisterExtension identifiers can't be fully type-attributed
            // here. The recipe's real-world target sources are always fully compiled, where this
            // resolves correctly; see
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
                    final NaturalistTestExtension db = NaturalistTestExtension.create();
                }
                """
            )
        );
    }

    @Test
    void methodLocalIsMarkedNotRewritten() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.NATURALIST_TEST_EXTENSION),
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
        );
    }

    @Test
    void extensionCreateIsNotFlagged() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.NATURALIST_TEST_EXTENSION),
            java(
                """
                import com.naturalist.data.NaturalistTestExtension;
                class T {
                    private final NaturalistTestExtension db = NaturalistTestExtension.create();
                }
                """   // no `after` => unchanged
            )
        );
    }
}
