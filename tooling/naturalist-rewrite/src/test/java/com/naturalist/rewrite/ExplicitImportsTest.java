package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcTestJava;

/**
 * Under the project style {@code com.naturalist.Style}, {@code EnforceArchitecture}'s {@code RemoveUnusedImports}
 * replaces a star import with the names the file uses through it: the style puts the fold count out of reach.
 */
class ExplicitImportsTest implements RewriteTest {

    private static final List<NamedStyles> STYLE = Environment.builder().scanRuntimeClasspath().build()
            .activateStyles("com.naturalist.Style");

    @Test
    void enforceArchitecture_expandsStarImports_andDropsUnusedOnes() {
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture")
                        .parser(JavaParser.fromJavaVersion().styles(STYLE)),
            srcTestJava(java(
            """
            package com.naturalist.x;

            import java.time.Duration;
            import java.util.*;

            import static java.util.Objects.*;

            class A {
                Map<String, List<String>> all = new HashMap<>();
                boolean empty(Object o) {
                    return isNull(o);
                }
            }
            """,
            """
            package com.naturalist.x;

            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;

            import static java.util.Objects.isNull;

            class A {
                Map<String, List<String>> all = new HashMap<>();
                boolean empty(Object o) {
                    return isNull(o);
                }
            }
            """)));
    }
}
