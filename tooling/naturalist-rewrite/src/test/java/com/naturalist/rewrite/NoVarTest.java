package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class NoVarTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoVar());
    }

    @Test
    void replacesALocalWithItsTypeAndImportsIt() {
        rewriteRun(srcMainJava(java(
                """
                        package com.naturalist.x;
                        
                        import java.util.ArrayList;
                        
                        class A {
                            int size() {
                                var names = new ArrayList<String>();
                                final var count = 3;
                                var text = "x";
                                var list = java.util.List.of(1L);
                                return names.size() + count + text.length() + list.size();
                            }
                        }
                        """,
                """
                        package com.naturalist.x;
                        
                        import java.util.ArrayList;
                        import java.util.List;
                        
                        class A {
                            int size() {
                                ArrayList<String> names = new ArrayList<String>();
                                final int count = 3;
                                String text = "x";
                                List<Long> list = List.of(1L);
                                return names.size() + count + text.length() + list.size();
                            }
                        }
                        """)));
    }

    @Test
    void replacesForEachAndTryWithResourcesVariables_withNestedAndWildcardTypes() {
        rewriteRun(srcTestJava(java(
                """
                        package com.naturalist.x;
                        
                        import java.io.ByteArrayOutputStream;
                        import java.util.List;
                        import java.util.Map;
                        
                        class B {
                            void walk(Map<String, List<? extends Number>> byName, int[] counts) throws Exception {
                                for (var entry : byName.entrySet()) {
                                    System.out.println(entry.getKey());
                                }
                                for (var c : counts) {
                                    System.out.println(c);
                                }
                                try (var out = new ByteArrayOutputStream()) {
                                    out.write(1);
                                }
                                var copy = counts.clone();
                            }
                        }
                        """,
                """
                        package com.naturalist.x;
                        
                        import java.io.ByteArrayOutputStream;
                        import java.util.List;
                        import java.util.Map;
                        
                        class B {
                            void walk(Map<String, List<? extends Number>> byName, int[] counts) throws Exception {
                                for (Map.Entry<String, List<? extends Number>> entry : byName.entrySet()) {
                                    System.out.println(entry.getKey());
                                }
                                for (int c : counts) {
                                    System.out.println(c);
                                }
                                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                    out.write(1);
                                }
                                int[] copy = counts.clone();
                            }
                        }
                        """)));
    }

    @Test
    void fullyQualifiesATypeWhoseSimpleNameIsAlreadyImported() {
        rewriteRun(srcMainJava(java(
                """
                        package com.naturalist.x;
                        
                        import java.awt.List;
                        
                        class C {
                            Object pick(List awt) {
                                var names = java.util.List.of("a");
                                return names.isEmpty() ? awt : names;
                            }
                        }
                        """,
                """
                        package com.naturalist.x;
                        
                        import java.awt.List;
                        
                        class C {
                            Object pick(List awt) {
                                java.util.List<String> names = java.util.List.of("a");
                                return names.isEmpty() ? awt : names;
                            }
                        }
                        """)));
    }

    @Test
    void importsATypeThatAppearsOnlyAsATypeArgument() {
        rewriteRun(srcTestJava(java(
                """
                        package com.naturalist.x;
                        
                        import java.nio.file.Files;
                        import java.util.stream.Stream;
                        
                        class G {
                            long count(java.io.File root) throws Exception {
                                try (var walk = Files.walk(root.toPath())) {
                                    return walk.count();
                                }
                            }
                        }
                        """,
                """
                        package com.naturalist.x;
                        
                        import java.nio.file.Files;
                        import java.nio.file.Path;
                        import java.util.stream.Stream;
                        
                        class G {
                            long count(java.io.File root) throws Exception {
                                try (Stream<Path> walk = Files.walk(root.toPath())) {
                                    return walk.count();
                                }
                            }
                        }
                        """)));
    }

    @Test
    void projectStyle_addsAnExplicitImport_neverFoldingThePackageIntoAStar() {
        List<NamedStyles> style = Environment.builder().scanRuntimeClasspath().build()
                .activateStyles("com.naturalist.Style");
        rewriteRun(spec -> spec.parser(JavaParser.fromJavaVersion().styles(style)), srcMainJava(java(
                """
                        package com.naturalist.x;
                        
                        import java.util.ArrayList;
                        import java.util.HashMap;
                        import java.util.HashSet;
                        import java.util.LinkedList;
                        import java.util.TreeMap;
                        
                        class F {
                            Object all(ArrayList<String> a, HashMap<String, String> b, HashSet<String> c, LinkedList<String> d,
                                       TreeMap<String, String> e) {
                                var first = java.util.Optional.of(a);
                                return first.isPresent() ? b : c.isEmpty() ? d : e;
                            }
                        }
                        """,
                """
                        package com.naturalist.x;
                        
                        import java.util.ArrayList;
                        import java.util.HashMap;
                        import java.util.HashSet;
                        import java.util.LinkedList;
                        import java.util.Optional;
                        import java.util.TreeMap;
                        
                        class F {
                            Object all(ArrayList<String> a, HashMap<String, String> b, HashSet<String> c, LinkedList<String> d,
                                       TreeMap<String, String> e) {
                                Optional<ArrayList<String>> first = Optional.of(a);
                                return first.isPresent() ? b : c.isEmpty() ? d : e;
                            }
                        }
                        """)));
    }

    @Test
    void marksATypeThatCannotBeNamed() {
        rewriteRun(srcMainJava(java(
                """
                        package com.naturalist.x;
                        
                        class D {
                            int anonymous() {
                                var counter = new Object() { int n = 1; };
                                return counter.n;
                            }
                        }
                        """,
                """
                        package com.naturalist.x;
                        
                        class D {
                            int anonymous() {
                                /*~~(%s)~~>*/var counter = new Object() { int n = 1; };
                                return counter.n;
                            }
                        }
                        """.formatted(NoVar.UNSPELLABLE))));
    }

    @Test
    void enforceArchitectureComposite_includesTheRecipe() {
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture")
                .parser(ProjectStyles.parser()), srcMainJava(java(
                """
                        package com.naturalist.x;
                        
                        class E {
                            String text() {
                                var text = "x";
                                return text;
                            }
                        }
                        """,
                """
                        package com.naturalist.x;
                        
                        class E {
                            String text() {
                                String text = "x";
                                return text;
                            }
                        }
                        """)));
    }
}
