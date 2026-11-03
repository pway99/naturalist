package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;

class NoSystemGetenvTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoSystemGetenv());
    }

    @Test
    void flagsGetenvOutsideTheSecretsPackage() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.console;
            class C { String k() { return System.getenv("X"); } }
            """,
            """
            package com.naturalist.console;
            class C { String k() { return /*~~(%s)~~>*/System.getenv("X"); } }
            """.formatted(NoSystemGetenv.MESSAGE))));
    }

    @Test
    void flagsTheNoArgumentForm() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.console;
            import java.util.Map;
            class C { Map<String, String> all() { return System.getenv(); } }
            """,
            """
            package com.naturalist.console;
            import java.util.Map;
            class C { Map<String, String> all() { return /*~~(%s)~~>*/System.getenv(); } }
            """.formatted(NoSystemGetenv.MESSAGE))));
    }

    @Test
    void doesNotFlagTheSecretsPackage() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.secrets;
            class EnvironmentSecretProvider { String k() { return System.getenv("X"); } }
            """)));
    }

    @Test
    void doesNotFlagTheStandingTestDatabaseWiring() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.persistence.test;
            final class RdbmsDataSource { static String env(String k) { return System.getenv(k); } }
            """)));
    }

    @Test
    void theExemptionIsPerClass_notPerPackage() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.persistence.test;
            class Other { String k() { return System.getenv("X"); } }
            """,
            """
            package com.naturalist.persistence.test;
            class Other { String k() { return /*~~(%s)~~>*/System.getenv("X"); } }
            """.formatted(NoSystemGetenv.MESSAGE))));
    }

    @Test
    void enforceArchitectureComposite_includesTheRecipe() {
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture"),
            srcMainJava(java(
            """
            package com.naturalist.console;
            class C { String k() { return System.getenv("X"); } }
            """,
            """
            package com.naturalist.console;
            class C { String k() { return /*~~(%s)~~>*/System.getenv("X"); } }
            """.formatted(NoSystemGetenv.MESSAGE))));
    }
}
