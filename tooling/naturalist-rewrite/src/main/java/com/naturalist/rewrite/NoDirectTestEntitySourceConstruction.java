package com.naturalist.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

public class NoDirectTestEntitySourceConstruction extends Recipe {

    private static final String BASE = "com.naturalist.data.TestEntitySource";

    @Override
    public String getDisplayName() {
        return "Acquire a TestEntitySource via NaturalistDatabase#getNamed, never `new`";
    }

    @Override
    public String getDescription() {
        return "Rewrites `new <X>TestEntitySource(db)` to `db.getNamed(<X>TestEntitySource.class)` "
             + "so every source is registered in the shared NaturalistDatabase (ADR-001).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // A plain JavaVisitor (not JavaIsoVisitor): the rewrite changes the node's own type from
        // J.NewClass to J.MethodInvocation, which the isomorphic visitor contract disallows.
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(nc.getType());
                if (type == null) {
                    return nc;
                }
                boolean isConcreteSource = !type.getFullyQualifiedName().equals(BASE)
                        && TypeUtils.isAssignableTo(BASE, nc.getType());
                if (!isConcreteSource) {
                    return nc;
                }
                if (nc.getArguments().size() != 1) {
                    return SearchResult.found(nc,
                        "unexpected TestEntitySource constructor arity; acquire via db.getNamed(...)");
                }
                Expression db = nc.getArguments().get(0);
                String simpleName = type.getClassName(); // simple name for top-level type
                return JavaTemplate
                        .builder("#{any(com.naturalist.data.NaturalistDatabase)}.getNamed(" + simpleName + ".class)")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), nc.getCoordinates().replace(), db);
            }
        };
    }
}
