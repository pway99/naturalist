package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class NoSubtypeJsonCreatorTest implements RewriteTest {

    private static final String JSON_CREATOR = """
            package com.fasterxml.jackson.annotation;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
            public @interface JsonCreator { }
            """;
    private static final String ENTITY_NAME = """
            package com.naturalist.ddd;
            public abstract class EntityName {
                protected EntityName(String value) { }
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
            }
            """;
    private static final String NUMERIC_NAMED_VALUE = """
            package com.naturalist.ddd;
            import java.math.BigDecimal;
            public interface NumericNamedValue extends NamedValue<BigDecimal> { }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoSubtypeJsonCreator());
    }

    private static SourceSpecs[] withSupertypes(SourceSpecs... subjects) {
        SourceSpecs[] all = new SourceSpecs[subjects.length + 5];
        all[0] = java(JSON_CREATOR);
        all[1] = java(ENTITY_NAME);
        all[2] = java(ENTITY_ID);
        all[3] = java(NAMED_VALUE);
        all[4] = java(NUMERIC_NAMED_VALUE);
        System.arraycopy(subjects, 0, all, 5, subjects.length);
        return all;
    }

    @Test
    void removesTheCreatorFromAName() {
        rewriteRun(withSupertypes(srcMainJava(java(
            """
            package com.naturalist.x;

            import com.fasterxml.jackson.annotation.JsonCreator;
            import com.naturalist.ddd.EntityName;

            public final class Slug extends EntityName {
                private Slug(String v) { super(v); }

                @JsonCreator
                public static Slug of(String v) {
                    return new Slug(v);
                }
            }
            """,
            """
            package com.naturalist.x;

            import com.naturalist.ddd.EntityName;

            public final class Slug extends EntityName {
                private Slug(String v) { super(v); }

                public static Slug of(String v) {
                    return new Slug(v);
                }
            }
            """))));
    }

    @Test
    void removesTheCreatorFromAnIdAndANumeric() {
        rewriteRun(withSupertypes(srcMainJava(
            java("""
                package com.naturalist.x;

                import com.fasterxml.jackson.annotation.JsonCreator;
                import com.naturalist.ddd.EntityId;
                import java.util.UUID;

                public final class ThingId extends EntityId {
                    private ThingId(UUID v) { super(v); }

                    @JsonCreator
                    public static ThingId of(UUID v) {
                        return new ThingId(v);
                    }
                }
                """,
                """
                package com.naturalist.x;

                import com.naturalist.ddd.EntityId;
                import java.util.UUID;

                public final class ThingId extends EntityId {
                    private ThingId(UUID v) { super(v); }

                    public static ThingId of(UUID v) {
                        return new ThingId(v);
                    }
                }
                """),
            java("""
                package com.naturalist.x;

                import com.fasterxml.jackson.annotation.JsonCreator;
                import com.naturalist.ddd.NumericNamedValue;
                import java.math.BigDecimal;

                public record Grams(BigDecimal value) implements NumericNamedValue {
                    @JsonCreator
                    public static Grams of(BigDecimal value) {
                        return new Grams(value);
                    }
                }
                """,
                """
                package com.naturalist.x;

                import com.naturalist.ddd.NumericNamedValue;
                import java.math.BigDecimal;

                public record Grams(BigDecimal value) implements NumericNamedValue {
                    public static Grams of(BigDecimal value) {
                        return new Grams(value);
                    }
                }
                """))));
    }

    @Test
    void leavesOtherTypesAlone() {
        rewriteRun(withSupertypes(srcMainJava(java(
            """
            package com.naturalist.x;

            import com.fasterxml.jackson.annotation.JsonCreator;

            public record Label(String text, String locale) {
                @JsonCreator
                public static Label of(String text, String locale) {
                    return new Label(text, locale);
                }
            }
            """))));
    }

    @Test
    void ignoresTestSources() {
        rewriteRun(withSupertypes(srcTestJava(java(
            """
            package com.naturalist.x;

            import com.fasterxml.jackson.annotation.JsonCreator;
            import com.naturalist.ddd.EntityName;

            final class FixtureName extends EntityName {
                private FixtureName(String v) { super(v); }

                @JsonCreator
                static FixtureName fromJson(String v) {
                    return new FixtureName("creator:" + v);
                }
            }
            """))));
    }

    @Test
    void enforceArchitectureComposite_includesTheRecipe() {
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture"),
            withSupertypes(srcMainJava(java(
            """
            package com.naturalist.x;

            import com.fasterxml.jackson.annotation.JsonCreator;
            import com.naturalist.ddd.NamedValue;

            public record Tag(String value) implements NamedValue<String> {
                @JsonCreator
                public static Tag of(String value) {
                    return new Tag(value);
                }
            }
            """,
            """
            package com.naturalist.x;

            import com.naturalist.ddd.NamedValue;

            public record Tag(String value) implements NamedValue<String> {
                public static Tag of(String value) {
                    return new Tag(value);
                }
            }
            """))));
    }
}
