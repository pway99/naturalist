package com.naturalist.rewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Comparator;

public class AcquireDatabaseViaExtension extends Recipe {

    private static final MethodMatcher CREATE =
        new MethodMatcher("com.naturalist.data.NaturalistDatabase create()");
    private static final String DB = "com.naturalist.data.NaturalistDatabase";
    private static final String EXT = "com.naturalist.data.NaturalistTestExtension";
    private static final String MARK =
        "hoist a @RegisterExtension NaturalistTestExtension field; "
      + "do not create a bare NaturalistDatabase in a test";

    @Override
    public String getDisplayName() {
        return "Obtain the test NaturalistDatabase from a @RegisterExtension NaturalistTestExtension";
    }

    @Override
    public String getDescription() {
        return "In test code, a bare NaturalistDatabase.create() is not reset per test. A field "
             + "initializer is retyped to NaturalistTestExtension with @RegisterExtension; every other "
             + "shape (locals, nested arguments, inline chains) is marked for a manual hoist.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
            new UsesMethod<>("com.naturalist.data.NaturalistDatabase create()"),
            new JavaIsoVisitor<ExecutionContext>() {

            /**
             * R2 is a test-only convention (the {@code @RegisterExtension} hoist only makes sense
             * inside JUnit test classes); it must never touch {@code src/main} — e.g. the
             * sanctioned {@code NaturalistDatabase.create()} callers in
             * {@code TestDataConfiguration}'s Spring {@code @Bean} or the main-source contract
             * base {@code TestEntitySourceTest} (ADR-001). Short-circuit on any source file whose
             * {@link JavaSourceSet} marker name is not {@code "test"}; a file with no marker at
             * all is treated as non-test (excluded), matching the conservative default.
             */
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    boolean isTest = ((JavaSourceFile) tree).getMarkers()
                        .findFirst(JavaSourceSet.class)
                        .map(s -> "test".equals(s.getName()))
                        .orElse(false);
                    if (!isTest) {
                        return (J) tree;
                    }
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
                // Case A: a field `NaturalistDatabase x = NaturalistDatabase.create();`
                if (isCaseAField(vd, getCursor())) {
                    return retypeCaseAField(vd, ctx);
                }
                return super.visitVariableDeclarations(vd, ctx);
            }

            /**
             * Two templates, NEVER one: a template whose first line is an annotation
             * ({@code @RegisterExtension}) followed by a declaration parses — once
             * {@code RegisterExtension} resolves to a real annotation type (true against a
             * fully-compiled classpath, though not against in-memory-only test stubs) — as TWO
             * statements, and {@code JavaTemplate.apply()} against a single-statement
             * {@code replace()} coordinate rejects that ("generated 2"). Reproduced against the real,
             * fully-compiled plants-console module.
             * <p>
             * Instead: (1) rebuild just the declaration ({@code Type name = Type.create()}, no
             * annotation) as one coherent template — a single statement, always safe — then (2) add
             * {@code @RegisterExtension} separately via the {@code addAnnotation()} coordinate. Never
             * combine an annotation and a declaration into one {@code replace()} template.
             * <p>
             * Rebuilding the declaration as one coherent template (rather than patching just the
             * type expression and initializer as two independent sub-node replacements) matters
             * beyond avoiding the crash: a sub-node-only patch leaves the untouched declarator
             * identifier's own cached type/fieldType and the {@code NamedVariable}'s
             * {@code variableType} still pointing at the old {@code NaturalistDatabase} type. That
             * stale metadata then fools {@code maybeRemoveImport}'s usage scan
             * ({@code RemoveImport} keys off {@code TypesInUse}, not printed text) into thinking
             * {@code NaturalistDatabase} is still referenced, so it refuses to drop the import. One
             * template call produces an internally consistent declarator/type/initializer with no
             * such stray.
             */
            private J.VariableDeclarations retypeCaseAField(J.VariableDeclarations vd, ExecutionContext ctx) {
                Cursor scope = getCursor();

                // The trailing `;` is load-bearing, not decoration: without it, this statement
                // parses fine when it is the class's only member (every unit-test fixture here),
                // but once a member follows it (every real Case-A field in practice — a test class
                // with only a field and no test methods doesn't exist), the missing terminator
                // makes the block-statement parse swallow part of the next member, so
                // JavaTemplate's "exactly one statement" check sees 2 and throws. Reproduced and
                // root-caused against the real, fully-compiled plants-console module.
                J.VariableDeclarations retypedDeclaration = JavaTemplate
                    .builder("NaturalistTestExtension #{} = NaturalistTestExtension.create();")
                    .contextSensitive()
                    .imports(EXT)
                    .build()
                    .apply(scope, vd.getCoordinates().replace(), vd.getVariables().get(0).getSimpleName());
                // The template text above carries no modifiers of its own; explicitly restore
                // whatever the original field had (e.g. `private final`) rather than hard-coding a
                // guess, so pre-existing modifiers survive the rewrite unchanged.
                retypedDeclaration = retypedDeclaration.withModifiers(vd.getModifiers());

                Cursor afterDeclarationScope = new Cursor(scope.getParentOrThrow(), retypedDeclaration);
                J.VariableDeclarations withAnnotation = JavaTemplate.builder("@RegisterExtension")
                    .contextSensitive()
                    .imports("org.junit.jupiter.api.extension.RegisterExtension")
                    .build()
                    .apply(afterDeclarationScope, retypedDeclaration.getCoordinates()
                        .addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));

                // JavaTemplate cannot fully type-attribute a reference to a type that exists only as
                // an in-memory parsed source (never compiled to a real classpath entry — true both
                // for these test stubs and, structurally, for any first-run rewrite where the target
                // repo's own compiled jars are stale/absent). So the usual type-driven "add import
                // only if referenced" machinery can't see this reference; add both imports
                // unconditionally instead (onlyIfReferenced=false).
                maybeAddImport(EXT, false);
                maybeAddImport("org.junit.jupiter.api.extension.RegisterExtension", false);
                maybeRemoveImport(DB);
                // Same-type replacement (field VariableDeclarations): normalize the added
                // @RegisterExtension annotation + retyped declaration formatting.
                return maybeAutoFormat(vd, withAnnotation.withPrefix(vd.getPrefix()), ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(mi, ctx);
                if (CREATE.matches(m)) {
                    // Case A initializers are handled in visitVariableDeclarations (which replaces
                    // this node before we get here); anything else that still matches is marked.
                    return SearchResult.found(m, MARK);
                }
                return m;
            }

            private boolean isCaseAField(J.VariableDeclarations vd, Cursor cursor) {
                if (!isField(cursor)) {
                    return false;
                }
                if (!TypeUtils.isOfClassType(vd.getType(), DB) || vd.getVariables().size() != 1) {
                    return false;
                }
                J.VariableDeclarations.NamedVariable var = vd.getVariables().get(0);
                return var.getInitializer() instanceof J.MethodInvocation
                    && CREATE.matches((J.MethodInvocation) var.getInitializer());
            }

            private boolean isField(Cursor cursor) {
                Cursor parent = cursor.getParentTreeCursor();
                if (!(parent.getValue() instanceof J.Block)) {
                    return false;
                }
                return parent.getParentTreeCursor().getValue() instanceof J.ClassDeclaration;
            }
        });
    }
}
