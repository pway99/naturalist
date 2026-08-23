package com.naturalist.rewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

public class NoSelectInIteration extends Recipe {

    static final String MESSAGE =
        "N+1 fan-out: repository or query select invoked inside an iteration construct; "
        + "call the batched sibling once instead";

    private static final String REPOSITORY = "com.naturalist.data.EntityRepository";
    private static final String QUERY = "com.naturalist.data.EntityQuery";
    private static final Set<String> WRITES = Set.of("insert", "update", "save");

    @Override
    public String getDisplayName() {
        return "Do not invoke a repository or query select inside a loop or stream fan-out";
    }

    @Override
    public String getDescription() {
        return "Flags an EntityRepository/EntityQuery select (excluding insert/update/save) that "
             + "appears lexically inside a loop or a per-element stream operation — the N+1 "
             + "fan-out pattern. Call the batched sibling (getByEntityNameSet / findByNameSet / a "
             + "domain getBy…Names) once instead. Cross-method and recursive fan-out are out of "
             + "scope (covered by the runtime select-count gate).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> visitor = new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    boolean isMain = ((JavaSourceFile) tree).getMarkers()
                        .findFirst(JavaSourceSet.class)
                        .map(s -> "main".equals(s.getName()))
                        .orElse(false);
                    if (!isMain) {
                        return (J) tree;
                    }
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(mi, ctx);
                if (!isSelectSite(m.getMethodType())) {
                    return m;
                }
                String kind = enclosingIterationKind(getCursor());
                if (kind == null) {
                    return m;
                }
                return SearchResult.found(m, MESSAGE);
            }
        };
        return Preconditions.check(
            Preconditions.or(new UsesType<>(REPOSITORY, true), new UsesType<>(QUERY, true)),
            visitor);
    }

    static boolean isSelectSite(JavaType.@Nullable Method methodType) {
        if (methodType == null) {
            return false;
        }
        JavaType.FullyQualified declaring = methodType.getDeclaringType();
        if (declaring == null) {
            return false;
        }
        boolean isPort = TypeUtils.isAssignableTo(REPOSITORY, declaring)
                      || TypeUtils.isAssignableTo(QUERY, declaring);
        return isPort && !WRITES.contains(methodType.getName());
    }

    /**
     * Walk from the select site up to (not past) the enclosing method declaration. Returns a
     * short label for the enclosing iteration construct, or {@code null} if none is found in the
     * same lexical method scope. (Stream fan-out is added in Task 2.)
     */
    static @Nullable String enclosingIterationKind(Cursor siteCursor) {
        Cursor cursor = siteCursor.getParent();
        while (cursor != null) {
            Object value = cursor.getValue();
            if (value instanceof J.ForEachLoop) {
                return "for-each-loop";
            }
            if (value instanceof J.ForLoop) {
                return "for-loop";
            }
            if (value instanceof J.WhileLoop) {
                return "while-loop";
            }
            if (value instanceof J.DoWhileLoop) {
                return "do-while-loop";
            }
            if (value instanceof J.MethodDeclaration || value instanceof J.ClassDeclaration) {
                return null;
            }
            cursor = cursor.getParent();
        }
        return null;
    }
}
