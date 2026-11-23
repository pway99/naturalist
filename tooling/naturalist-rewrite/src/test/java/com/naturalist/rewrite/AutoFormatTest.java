package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;


import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;

/**
 * The root pom activates IntelliJ's style and then the project style; {@code EnforceArchitecture} formats every
 * source under them, so the code base has one format.
 */
class AutoFormatTest implements RewriteTest {

    @Test
    void bothActiveStylesResolveByName() {
        assertThat(ProjectStyles.ACTIVE).extracting(NamedStyles::getName)
                .containsExactly("org.openrewrite.java.IntelliJ", "com.naturalist.Style");
    }

    @Test
    void enforceArchitecture_formatsToIntelliJ() {
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture")
                        .parser(ProjectStyles.parser()),
                srcMainJava(java(
                        """
                                package com.naturalist.x;
                                
                                class A {
                                  int  size( int a,int b ){
                                      if(a>b){ return a; }
                                    return b ;
                                  }
                                }
                                """,
                        """
                                package com.naturalist.x;
                                
                                class A {
                                    int size(int a, int b) {
                                        if (a > b) {
                                            return a;
                                        }
                                        return b;
                                    }
                                }
                                """)));
    }
}
