package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

/**
 * Fixtures that must trip the recipe build their credential-shaped text by concatenation,
 * so this file never trips the repo-wide dry run itself.
 */
class NoNamedCredentialsTest implements RewriteTest {

    private static final String VENDOR_VARIABLE = "OPENAI" + "_API_KEY";
    private static final String KEBAB_NAME = "model" + "-api-key";
    private static final String KEY_PREFIX = "sk-" + "ant-api03-abc";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoNamedCredentials());
    }

    @Test
    void flagsAVendorKeyVariableInALiteral() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.console;
            class C { String k = "%s"; }
            """.formatted(VENDOR_VARIABLE),
            """
            /*~~(%s)~~>*/package com.naturalist.console;
            class C { String k = "%s"; }
            """.formatted(NoNamedCredentials.MESSAGE, VENDOR_VARIABLE))));
    }

    @Test
    void flagsAKeyNameInAComment_inTestSourcesToo() {
        rewriteRun(srcTestJava(java(
            """
            package com.naturalist.console;
            // reads the %s secret
            class CTest { }
            """.formatted(KEBAB_NAME),
            """
            /*~~(%s)~~>*/package com.naturalist.console;
            // reads the %s secret
            class CTest { }
            """.formatted(NoNamedCredentials.MESSAGE, KEBAB_NAME))));
    }

    @Test
    void flagsAVendorKeyPrefix() {
        rewriteRun(srcTestJava(java(
            """
            package com.naturalist.console;
            class CTest { String v = "%s"; }
            """.formatted(KEY_PREFIX),
            """
            /*~~(%s)~~>*/package com.naturalist.console;
            class CTest { String v = "%s"; }
            """.formatted(NoNamedCredentials.MESSAGE, KEY_PREFIX))));
    }

    @Test
    void doesNotFlagNeutralSecretNames_orTheSdkBuilderMethod() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.vision.anthropic;
            class C {
                String name = "model-credential";
                String variable = "MODEL_CREDENTIAL";
                Object build(Builder b, String v) { return b.apiKey(v); }
                interface Builder { Object apiKey(String v); }
            }
            """)));
    }

    @Test
    void enforceArchitectureComposite_includesTheRecipe() {
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture"),
            srcMainJava(java(
            """
            package com.naturalist.console;
            class C { String k = "%s"; }
            """.formatted(VENDOR_VARIABLE),
            """
            /*~~(%s)~~>*/package com.naturalist.console;
            class C { String k = "%s"; }
            """.formatted(NoNamedCredentials.MESSAGE, VENDOR_VARIABLE))));
    }
}
