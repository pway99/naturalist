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
    void flagsSelectInForLoop() {
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
                            for (int i = 0; i < names.size(); i++) {
                                repo.getByName(names.get(i));
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
                            for (int i = 0; i < names.size(); i++) {
                                %srepo.getByName(names.get(i));
                            }
                        }
                    }
                    """.formatted(MARK)
                )
            )
        );
    }

    @Test
    void flagsSelectInDoWhileLoop() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(String n, boolean cond) {
                            do {
                                repo.getByName(n);
                            } while (cond);
                        }
                    }
                    """,
                    """
                    package com.naturalist.data;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(String n, boolean cond) {
                            do {
                                %srepo.getByName(n);
                            } while (cond);
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
    void doesNotFlagBatchedCallInForEachHeader() {
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
                            for (String s : repo.getByEntityNameSet(names)) {
                            }
                        }
                    }
                    """  // no `after` => unchanged — batched select runs once, in the header
                )
            )
        );
    }

    @Test
    void doesNotFlagSelectInForLoopInit() {
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
                            for (int i = repo.getByEntityNameSet(names).size(); i > 0; i--) {
                            }
                        }
                    }
                    """  // no `after` => unchanged — the init clause runs once, not per-iteration
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
    void flagsRepositorySelectMethodReferenceInStreamMap() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    import java.util.Optional;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        List<Optional<String>> load(List<String> names) {
                            return names.stream().map(repo::getByName).toList();
                        }
                    }
                    """,
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    import java.util.Optional;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        List<Optional<String>> load(List<String> names) {
                            return names.stream().map(%srepo::getByName).toList();
                        }
                    }
                    """.formatted(MARK)
                )
            )
        );
    }

    @Test
    void flagsQuerySelectLambdaInStreamForEach() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_QUERY),
            java(NaturalistTypeStubs.FOO_QUERY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooQuery query;
                        Q(FooQuery query) { this.query = query; }
                        void load(List<String> names) {
                            names.stream().forEach(n -> query.getByName(n));
                        }
                    }
                    """,
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooQuery query;
                        Q(FooQuery query) { this.query = query; }
                        void load(List<String> names) {
                            names.stream().forEach(n -> %squery.getByName(n));
                        }
                    }
                    """.formatted(MARK)
                )
            )
        );
    }

    @Test
    void flagsRepositorySelectInStreamFilterPredicate() {
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
                        List<String> load(List<String> names) {
                            return names.stream().filter(n -> repo.getByName(n).isPresent()).toList();
                        }
                    }
                    """,
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        List<String> load(List<String> names) {
                            return names.stream().filter(n -> %srepo.getByName(n).isPresent()).toList();
                        }
                    }
                    """.formatted(MARK)
                )
            )
        );
    }

    @Test
    void doesNotFlagSelectInNonFanOutLambda() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    // Optional.orElseGet supplier is not a per-element fan-out; a single deferred
                    // lookup, not an N+1. Must stay clean.
                    """
                    package com.naturalist.data;
                    import java.util.Optional;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        String load(Optional<String> maybe, String fallback) {
                            return maybe.orElseGet(() -> repo.getByName(fallback).orElse(null));
                        }
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
