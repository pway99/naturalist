package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class NoSelectInIterationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoSelectInIteration());
    }

    private static final String MARK = "/*~~(" + NoSelectInIteration.MESSAGE + ")~~>*/";

    @Test
    void flagsRepositorySelectInForEachLoop() {
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
                                %srepo.getByName(n);
                            }
                        }
                    }
                    """.formatted(MARK)
                )
            )
        );
    }

    @Test
    void flagsDomainSpecificSelectInWhileLoop() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.Iterator;
                    import java.util.Set;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(Iterator<Set<String>> it) {
                            while (it.hasNext()) {
                                repo.getByParentNames(it.next());
                            }
                        }
                    }
                    """,
                    """
                    package com.naturalist.data;
                    import java.util.Iterator;
                    import java.util.Set;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(Iterator<Set<String>> it) {
                            while (it.hasNext()) {
                                %srepo.getByParentNames(it.next());
                            }
                        }
                    }
                    """.formatted(MARK)
                )
            )
        );
    }

    @Test
    void doesNotFlagBatchedSelectOutsideLoop() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.Set;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(Set<String> names) {
                            repo.getByEntityNameSet(names);
                        }
                    }
                    """  // no `after` => unchanged
                )
            )
        );
    }

    @Test
    void doesNotFlagWriteInLoop() {
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
                        void seed(List<String> names) {
                            for (String n : names) {
                                repo.insert(n);
                            }
                        }
                    }
                    """
                )
            )
        );
    }

    @Test
    void doesNotFlagSelectInHelperCalledFromLoop() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    // the loop and the select are in different methods — cross-method fan-out,
                    // delegated to the AspectJ runtime gate; this static recipe must not flag it.
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(List<String> names) {
                            for (String n : names) {
                                one(n);
                            }
                        }
                        String one(String n) { return repo.getByName(n).orElse(null); }
                    }
                    """
                )
            )
        );
    }

    @Test
    void doesNotFlagLoopSelectInTestSource() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcTestJava(
                java(
                    // legitimate test arrange/assert loop — main-source-only guard leaves it alone.
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class QTest {
                        private final FooRepository repo = null;
                        void assertsEach(List<String> names) {
                            for (String n : names) {
                                repo.getByName(n);
                            }
                        }
                    }
                    """
                )
            )
        );
    }
}
