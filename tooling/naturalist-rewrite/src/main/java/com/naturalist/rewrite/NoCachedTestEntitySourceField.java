package com.naturalist.rewrite;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

public class NoCachedTestEntitySourceField extends Recipe {

    private static final String BASE = "com.naturalist.data.TestEntitySource";
    private static final String MESSAGE =
        "cache TestEntitySource in a field; fetch via db.getNamed(...) inside each test so sources reset per test";

    @Override
    public String getDisplayName() {
        return "Do not cache a TestEntitySource in a field";
    }

    @Override
    public String getDescription() {
        return "A TestEntitySource-typed field is populated at construction, before the extension's "
             + "@BeforeEach registry reset, so it goes stale. Fetch from NaturalistDatabase#getNamed "
             + "inside each test instead. Method-locals and getNamed-returning helpers are allowed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
                J.VariableDeclarations v = super.visitVariableDeclarations(vd, ctx);
                if (!isField(getCursor())) {
                    return v;
                }
                JavaType elementType = v.getType();
                if (elementType != null
                        && !TypeUtils.isOfClassType(elementType, BASE)   // not the abstract base itself
                        && TypeUtils.isAssignableTo(BASE, elementType)) {
                    return SearchResult.found(v, MESSAGE);
                }
                return v;
            }

            /** A field declaration sits directly in a class body block. */
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
