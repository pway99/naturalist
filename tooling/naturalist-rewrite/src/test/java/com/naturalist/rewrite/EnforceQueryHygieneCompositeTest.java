package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;

class EnforceQueryHygieneCompositeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("com.naturalist.EnforceQueryHygiene");
    }

    @Test
    void compositeFlagsAnNPlusOne() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(List<String> names) {
                            for (String n : names) {
                                repo.getByName(n);
                            }
                        }
                    }
                    """,
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(List<String> names) {
                            for (String n : names) {
                                /*~~(%s)~~>*/repo.getByName(n);
                            }
                        }
                    }
                    """.formatted(NoSelectInIteration.MESSAGE)
                )
            )
        );
    }
}
