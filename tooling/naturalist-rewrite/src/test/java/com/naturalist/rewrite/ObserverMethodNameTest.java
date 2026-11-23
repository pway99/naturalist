package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class ObserverMethodNameTest implements RewriteTest {

    private static final String OBSERVER = """
            package com.naturalist.observability;
            import java.util.function.Consumer;
            public final class Observer {
                public static Observer forClass(Class<?> c) { return new Observer(); }
                public MethodObserver forMethod(String methodName) { return new MethodObserver(); }
                public Object arguments(String methodName, Consumer<Object> constraints) { return null; }
                public Object state(String methodName, Consumer<Object> constraints) { return null; }
                public void noticeError(String methodName, String name, Throwable failure, Object handling) { }
            }
            """;
    private static final String METHOD_OBSERVER = """
            package com.naturalist.observability;
            public final class MethodObserver {
                public void noticeError(String name, Throwable failure, Object handling) { }
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ObserverMethodName());
    }

    private static SourceSpecs[] withObserver(SourceSpecs subject) {
        return new SourceSpecs[]{java(OBSERVER), java(METHOD_OBSERVER), subject};
    }

    @Test
    void marksEachObservationNamingAnotherMethod() {
        rewriteRun(withObserver(srcMainJava(java(
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            class A {
                private static final Observer OBSERVER = Observer.forClass(A.class);
                void renamed(Object o, Exception e) {
                    OBSERVER.forMethod("before");
                    OBSERVER.arguments("before", c -> { });
                    OBSERVER.state("before", c -> { });
                    OBSERVER.noticeError("before", "failed", e, null);
                }
            }
            """,
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            class A {
                private static final Observer OBSERVER = Observer.forClass(A.class);
                void renamed(Object o, Exception e) {
                    /*~~(%1$s)~~>*/OBSERVER.forMethod("before");
                    /*~~(%1$s)~~>*/OBSERVER.arguments("before", c -> { });
                    /*~~(%1$s)~~>*/OBSERVER.state("before", c -> { });
                    /*~~(%1$s)~~>*/OBSERVER.noticeError("before", "failed", e, null);
                }
            }
            """.formatted(ObserverMethodName.message("before", "renamed"))))));
    }

    @Test
    void acceptsTheEnclosingMethodsName_includingFromALambda() {
        rewriteRun(withObserver(srcTestJava(java(
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            import java.util.List;
            class B {
                private static final Observer observer = Observer.forClass(B.class);
                void allValid(List<Object> values) {
                    observer.forMethod("allValid");
                    values.forEach(v -> observer.arguments("allValid", c -> { }));
                }
            }
            """))));
    }

    @Test
    void aConstructorObservesAsConstructor() {
        rewriteRun(withObserver(srcMainJava(java(
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            class C {
                private static final Observer observer = Observer.forClass(C.class);
                C(Object a) {
                    observer.arguments("constructor", c -> { });
                }
                C(Object a, Object b) {
                    observer.arguments("construct", c -> { });
                }
            }
            """,
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            class C {
                private static final Observer observer = Observer.forClass(C.class);
                C(Object a) {
                    observer.arguments("constructor", c -> { });
                }
                C(Object a, Object b) {
                    /*~~(%s)~~>*/observer.arguments("construct", c -> { });
                }
            }
            """.formatted(ObserverMethodName.message("construct", "constructor"))))));
    }

    @Test
    void aRecordsCompactConstructorObservesAsConstructor() {
        rewriteRun(withObserver(srcMainJava(java(
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            record D(String value) {
                private static final Observer observer = Observer.forClass(D.class);
                D {
                    observer.arguments("constructor", c -> { });
                }
            }
            """))));
    }

    @Test
    void anAnonymousClassMethodIsTheEnclosingMethod() {
        rewriteRun(withObserver(srcTestJava(java(
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            class E {
                private static final Observer observer = Observer.forClass(E.class);
                Runnable outer() {
                    return new Runnable() {
                        @Override
                        public void run() {
                            observer.forMethod("outer");
                        }
                    };
                }
            }
            """,
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            class E {
                private static final Observer observer = Observer.forClass(E.class);
                Runnable outer() {
                    return new Runnable() {
                        @Override
                        public void run() {
                            /*~~(%s)~~>*/observer.forMethod("outer");
                        }
                    };
                }
            }
            """.formatted(ObserverMethodName.message("outer", "run"))))));
    }

    @Test
    void leavesAMethodObserversFailureName_andNonLiteralNames_alone() {
        rewriteRun(withObserver(srcMainJava(java(
            """
            package com.naturalist.x;
            import com.naturalist.observability.MethodObserver;
            import com.naturalist.observability.Observer;
            class F {
                private static final Observer OBSERVER = Observer.forClass(F.class);
                private static final String NAME = "elsewhere";
                void read(Exception e) {
                    MethodObserver methodObserver = OBSERVER.forMethod("read");
                    methodObserver.noticeError("opened", e, null);
                    OBSERVER.state(NAME, c -> { });
                }
            }
            """))));
    }

    @Test
    void enforceArchitectureComposite_includesTheRecipe() {
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture"),
            withObserver(srcMainJava(java(
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            class G {
                private static final Observer OBSERVER = Observer.forClass(G.class);
                void renamed() {
                    OBSERVER.forMethod("before");
                }
            }
            """,
            """
            package com.naturalist.x;
            import com.naturalist.observability.Observer;
            class G {
                private static final Observer OBSERVER = Observer.forClass(G.class);
                void renamed() {
                    /*~~(%s)~~>*/OBSERVER.forMethod("before");
                }
            }
            """.formatted(ObserverMethodName.message("before", "renamed"))))));
    }
}
