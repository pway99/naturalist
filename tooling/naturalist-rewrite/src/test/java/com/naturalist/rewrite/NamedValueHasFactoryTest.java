package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class NamedValueHasFactoryTest implements RewriteTest {

    private static final String ENTITY_NAME = """
            package com.naturalist.ddd;
            public abstract class EntityName {
                protected EntityName(String value) { }
                protected abstract int maxLength();
            }
            """;
    private static final String ENTITY_ID = """
            package com.naturalist.ddd;
            import java.util.UUID;
            public abstract class EntityId {
                protected EntityId(UUID value) { }
            }
            """;
    private static final String NAMED_VALUE = """
            package com.naturalist.ddd;
            public interface NamedValue<T> {
                T value();
                boolean isValid();
            }
            """;
    private static final String NUMERIC_NAMED_VALUE = """
            package com.naturalist.ddd;
            import java.math.BigDecimal;
            public interface NumericNamedValue extends NamedValue<BigDecimal> { }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NamedValueHasFactory());
    }

    private static SourceSpecs[] withSupertypes(SourceSpecs... subjects) {
        SourceSpecs[] all = new SourceSpecs[subjects.length + 4];
        all[0] = java(ENTITY_NAME);
        all[1] = java(ENTITY_ID);
        all[2] = java(NAMED_VALUE);
        all[3] = java(NUMERIC_NAMED_VALUE);
        System.arraycopy(subjects, 0, all, 4, subjects.length);
        return all;
    }

    @Test
    void flagsANameWithoutAFactory() {
        rewriteRun(withSupertypes(srcMainJava(java(
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.EntityName;
                        final class BareName extends EntityName { BareName(String v) { super(v); } protected int maxLength() { return 8; } }
                        """,
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.EntityName;
                        /*~~(%s)~~>*/final class BareName extends EntityName { BareName(String v) { super(v); } protected int maxLength() { return 8; } }
                        """.formatted(NamedValueHasFactory.MESSAGE)))));
    }

    @Test
    void flagsAWrongParameterType() {
        rewriteRun(withSupertypes(srcMainJava(java(
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.NamedValue;
                        public record Count(String value) implements NamedValue<String> {
                            public static Count of(int value) { return new Count(String.valueOf(value)); }
                            public boolean isValid() { return true; }
                        }
                        """,
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.NamedValue;
                        /*~~(%s)~~>*/public record Count(String value) implements NamedValue<String> {
                            public static Count of(int value) { return new Count(String.valueOf(value)); }
                            public boolean isValid() { return true; }
                        }
                        """.formatted(NamedValueHasFactory.MESSAGE)))));
    }

    @Test
    void flagsANonPublicFactory() {
        rewriteRun(withSupertypes(srcMainJava(java(
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.NamedValue;
                        public record Tag(String value) implements NamedValue<String> {
                            static Tag of(String value) { return new Tag(value); }
                            public boolean isValid() { return true; }
                        }
                        """,
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.NamedValue;
                        /*~~(%s)~~>*/public record Tag(String value) implements NamedValue<String> {
                            static Tag of(String value) { return new Tag(value); }
                            public boolean isValid() { return true; }
                        }
                        """.formatted(NamedValueHasFactory.MESSAGE)))));
    }

    @Test
    void flagsANumericWithoutAFactory() {
        rewriteRun(withSupertypes(srcMainJava(java(
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.NumericNamedValue;
                        import java.math.BigDecimal;
                        public record Depth(BigDecimal value) implements NumericNamedValue {
                            public boolean isValid() { return true; }
                        }
                        """,
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.NumericNamedValue;
                        import java.math.BigDecimal;
                        /*~~(%s)~~>*/public record Depth(BigDecimal value) implements NumericNamedValue {
                            public boolean isValid() { return true; }
                        }
                        """.formatted(NamedValueHasFactory.MESSAGE)))));
    }

    @Test
    void flagsASecondQualifyingFactory() {
        rewriteRun(withSupertypes(srcMainJava(java(
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.EntityName;
                        import java.util.UUID;
                        public final class TwoWays extends EntityName {
                            private TwoWays(String v) { super(v); }
                            public static TwoWays of(String v) { return new TwoWays(v); }
                            public static TwoWays of(UUID v) { return new TwoWays(v.toString()); }
                            protected int maxLength() { return 64; }
                        }
                        """,
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.EntityName;
                        import java.util.UUID;
                        /*~~(%s)~~>*/public final class TwoWays extends EntityName {
                            private TwoWays(String v) { super(v); }
                            public static TwoWays of(String v) { return new TwoWays(v); }
                            public static TwoWays of(UUID v) { return new TwoWays(v.toString()); }
                            protected int maxLength() { return 64; }
                        }
                        """.formatted(NamedValueHasFactory.AMBIGUOUS)))));
    }

    @Test
    void acceptsEachFactoryShape() {
        rewriteRun(withSupertypes(srcMainJava(
                java("""
                        package com.naturalist.x;
                        import com.naturalist.ddd.EntityName;
                        public final class Slug extends EntityName {
                            private Slug(String v) { super(v); }
                            public static Slug of(String v) { return new Slug(v); }
                            protected int maxLength() { return 8; }
                        }
                        """),
                java("""
                        package com.naturalist.x;
                        import com.naturalist.ddd.NumericNamedValue;
                        import java.math.BigDecimal;
                        public record Grams(BigDecimal value) implements NumericNamedValue {
                            public static Grams of(BigDecimal value) { return new Grams(value); }
                            public boolean isValid() { return true; }
                        }
                        """),
                java("""
                        package com.naturalist.x;
                        import com.naturalist.ddd.EntityId;
                        import java.util.UUID;
                        public final class ThingId extends EntityId {
                            private ThingId(UUID v) { super(v); }
                            public static ThingId of(UUID v) { return new ThingId(v); }
                        }
                        """))));
    }

    @Test
    void acceptsAComposerBesideTheFactory() {
        rewriteRun(withSupertypes(srcMainJava(java(
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.EntityName;
                        public final class Stage extends EntityName {
                            private Stage(String v) { super(v); }
                            public static Stage of(String v) { return new Stage(v); }
                            public static Stage of(Stage a, Stage b) { return new Stage(a + "-" + b); }
                            protected int maxLength() { return 64; }
                        }
                        """))));
    }

    @Test
    void ignoresAbstractTypesAndInterfaces() {
        rewriteRun(withSupertypes(srcMainJava(
                java("""
                        package com.naturalist.x;
                        import com.naturalist.ddd.EntityName;
                        public abstract class RankedName extends EntityName { protected RankedName(String v) { super(v); } }
                        """),
                java("""
                        package com.naturalist.x;
                        import com.naturalist.ddd.NamedValue;
                        public interface Labelled extends NamedValue<String> { }
                        """))));
    }

    @Test
    void ignoresTestSources() {
        rewriteRun(withSupertypes(srcTestJava(java(
                """
                        package com.naturalist.x;
                        import com.naturalist.ddd.EntityName;
                        final class FixtureName extends EntityName { FixtureName(String v) { super(v); } protected int maxLength() { return 8; } }
                        """))));
    }

    @Test
    void enforceArchitectureComposite_includesTheRecipe() {
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture")
                        .parser(ProjectStyles.parser(ENTITY_NAME, ENTITY_ID, NAMED_VALUE, NUMERIC_NAMED_VALUE)),
                srcMainJava(java(
                        """
                                package com.naturalist.x;
                                
                                import com.naturalist.ddd.EntityName;
                                
                                final class BareName extends EntityName {
                                    BareName(String v) {
                                        super(v);
                                    }
                                
                                    protected int maxLength() {
                                        return 8;
                                    }
                                }
                                """,
                        """
                                package com.naturalist.x;
                                
                                import com.naturalist.ddd.EntityName;
                                
                                /*~~(%s)~~>*/final class BareName extends EntityName {
                                    BareName(String v) {
                                        super(v);
                                    }
                                
                                    protected int maxLength() {
                                        return 8;
                                    }
                                }
                                """.formatted(NamedValueHasFactory.MESSAGE))));
    }
}
