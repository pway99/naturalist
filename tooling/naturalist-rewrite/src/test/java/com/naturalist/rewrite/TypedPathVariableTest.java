package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class TypedPathVariableTest implements RewriteTest {

    private static final String WEB = """
            package org.springframework.web.bind.annotation;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @interface Marker { }
            """;
    private static final String GET_MAPPING = """
            package org.springframework.web.bind.annotation;
            public @interface GetMapping { String[] value() default {}; }
            """;
    private static final String POST_MAPPING = """
            package org.springframework.web.bind.annotation;
            public @interface PostMapping { String[] value() default {}; }
            """;
    private static final String PATH_VARIABLE = """
            package org.springframework.web.bind.annotation;
            public @interface PathVariable { String value() default ""; boolean required() default true; }
            """;
    private static final String REQUEST_PARAM = """
            package org.springframework.web.bind.annotation;
            public @interface RequestParam {
                String value() default ""; String name() default ""; boolean required() default true;
            }
            """;
    private static final String ENTITY_NAME = """
            package com.naturalist.ddd;
            public abstract class EntityName {
                protected EntityName(String value) { }
                public String value() { return null; }
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
            public interface NamedValue<T> { T value(); }
            """;
    private static final String NUMERIC_NAMED_VALUE = """
            package com.naturalist.ddd;
            import java.math.BigDecimal;
            public interface NumericNamedValue extends NamedValue<BigDecimal> { }
            """;
    private static final String SLUG = """
            package com.naturalist.x;
            import com.naturalist.ddd.EntityName;
            public final class Slug extends EntityName {
                private Slug(String v) { super(v); }
                public static Slug of(String v) { return new Slug(v); }
            }
            """;
    private static final String THING_ID = """
            package com.naturalist.x;
            import com.naturalist.ddd.EntityId;
            import java.util.UUID;
            public final class ThingId extends EntityId {
                private ThingId(UUID v) { super(v); }
                public static ThingId of(UUID v) { return new ThingId(v); }
            }
            """;
    private static final String GRAMS = """
            package com.naturalist.x;
            import com.naturalist.ddd.NumericNamedValue;
            import java.math.BigDecimal;
            public record Grams(BigDecimal value) implements NumericNamedValue {
                public static Grams of(BigDecimal value) { return new Grams(value); }
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TypedPathVariable());
    }

    private static SourceSpecs[] withTypes(SourceSpecs... subjects) {
        String[] fixtures = {WEB, GET_MAPPING, POST_MAPPING, PATH_VARIABLE, REQUEST_PARAM,
                ENTITY_NAME, ENTITY_ID, NAMED_VALUE, NUMERIC_NAMED_VALUE, SLUG, THING_ID, GRAMS};
        SourceSpecs[] all = new SourceSpecs[subjects.length + fixtures.length];
        for (int i = 0; i < fixtures.length; i++) {
            all[i] = java(fixtures[i]);
        }
        System.arraycopy(subjects, 0, all, fixtures.length, subjects.length);
        return all;
    }

    @Test
    void marksAStringPathVariableReadIntoAName() {
        rewriteRun(withTypes(srcMainJava(java(
            """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            class Controller {
                @GetMapping("/{name}")
                String detail(@PathVariable String name) {
                    return Slug.of(name).value();
                }
            }
            """,
            """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            class Controller {
                @GetMapping("/{name}")
                String detail(/*~~(%s)~~>*/@PathVariable String name) {
                    return Slug.of(name).value();
                }
            }
            """.formatted(TypedPathVariable.TYPED)))));
    }

    @Test
    void marksAStringRequestParamParsedIntoAnIdOrANumber() {
        rewriteRun(withTypes(srcMainJava(java(
            """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            import java.math.BigDecimal;
            import java.util.UUID;
            class Controller {
                @PostMapping("/weigh")
                String weigh(@RequestParam("id") String id, @RequestParam(name = "grams", required = false) String grams) {
                    ThingId thing = ThingId.of(UUID.fromString(id));
                    Grams weight = Grams.of(new BigDecimal(grams));
                    return thing + " " + weight;
                }
            }
            """,
            """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            import java.math.BigDecimal;
            import java.util.UUID;
            class Controller {
                @PostMapping("/weigh")
                String weigh(/*~~(%1$s)~~>*/@RequestParam("id") String id, /*~~(%1$s)~~>*/@RequestParam(name = "grams", required = false) String grams) {
                    ThingId thing = ThingId.of(UUID.fromString(id));
                    Grams weight = Grams.of(new BigDecimal(grams));
                    return thing + " " + weight;
                }
            }
            """.formatted(TypedPathVariable.TYPED)))));
    }

    @Test
    void marksARequiredIdOrNumberParameter() {
        rewriteRun(withTypes(srcMainJava(java(
            """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            class Controller {
                @PostMapping("/weigh")
                String weigh(@RequestParam("id") ThingId id, @RequestParam("grams") Grams grams) {
                    return id + " " + grams;
                }
            }
            """,
            """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            class Controller {
                @PostMapping("/weigh")
                String weigh(/*~~(%1$s)~~>*/@RequestParam("id") ThingId id, /*~~(%1$s)~~>*/@RequestParam("grams") Grams grams) {
                    return id + " " + grams;
                }
            }
            """.formatted(TypedPathVariable.REQUIRED)))));
    }

    @Test
    void acceptsTypedParametersThatMayReadNull() {
        rewriteRun(withTypes(srcMainJava(java(
            """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            import java.util.Optional;
            class Controller {
                @GetMapping("/{name}")
                String detail(@PathVariable Slug name,
                              @RequestParam(name = "id", required = false) ThingId id,
                              @RequestParam("grams") Optional<Grams> grams) {
                    return name.value() + id + grams;
                }
            }
            """))));
    }

    @Test
    void leavesPlainStringsAndNonHandlersAlone() {
        rewriteRun(withTypes(srcMainJava(java(
            """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            class Controller {
                @GetMapping("/search")
                String search(@RequestParam("q") String q, @RequestParam("name") String name) {
                    String other = name.trim();
                    return q + Slug.of(other).value();
                }

                String helper(String name) {
                    return Slug.of(name).value();
                }
            }
            """))));
    }

    @Test
    void ignoresTestSources() {
        rewriteRun(withTypes(srcTestJava(java(
            """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            class FakeController {
                @GetMapping("/{name}")
                String detail(@PathVariable String name) {
                    return Slug.of(name).value();
                }
            }
            """))));
    }

    @Test
    void namedValueEdgesComposite_includesTheRecipe_andEnforceArchitectureDoesNotYet() {
        String controller = """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            class Controller {
                @GetMapping("/{name}")
                String detail(@PathVariable String name) {
                    return Slug.of(name).value();
                }
            }
            """;
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.NamedValueEdges"),
            withTypes(srcMainJava(java(controller, """
            package com.naturalist.x;
            import org.springframework.web.bind.annotation.*;
            class Controller {
                @GetMapping("/{name}")
                String detail(/*~~(%s)~~>*/@PathVariable String name) {
                    return Slug.of(name).value();
                }
            }
            """.formatted(TypedPathVariable.TYPED)))));
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture"),
            withTypes(srcMainJava(java(controller))));
    }
}
