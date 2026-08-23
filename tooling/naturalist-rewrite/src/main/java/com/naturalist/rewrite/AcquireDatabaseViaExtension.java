package com.naturalist.rewrite;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

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
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
                // Case A: a field `NaturalistDatabase x = NaturalistDatabase.create();`
                if (isCaseAField(vd, getCursor())) {
                    J.VariableDeclarations retyped = JavaTemplate
                        .builder("@RegisterExtension\n"
                               + "final NaturalistTestExtension #{} = NaturalistTestExtension.create()")
                        .contextSensitive()
                        .imports(EXT, "org.junit.jupiter.api.extension.RegisterExtension")
                        .build()
                        .apply(getCursor(), vd.getCoordinates().replace(),
                               vd.getVariables().get(0).getSimpleName());
                    // JavaTemplate cannot fully type-attribute a reference to a type that exists
                    // only as an in-memory parsed source (never compiled to a real classpath entry
                    // — true both for these test stubs and, structurally, for any first-run rewrite
                    // where the target repo's own compiled jars are stale/absent). So the usual
                    // type-driven "add import only if referenced" machinery can't see this
                    // reference; add both imports unconditionally instead (onlyIfReferenced=false).
                    maybeAddImport(EXT, false);
                    maybeAddImport("org.junit.jupiter.api.extension.RegisterExtension", false);
                    maybeRemoveImport(DB);
                    // Same-type replacement (field VariableDeclarations): normalize the
                    // added @RegisterExtension annotation + retyped declaration formatting.
                    return maybeAutoFormat(vd, retyped.withPrefix(vd.getPrefix()), ctx);
                }
                return super.visitVariableDeclarations(vd, ctx);
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
        };
    }
}
