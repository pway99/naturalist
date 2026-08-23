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

            /**
             * This is a main-source invariant: test loops legitimately fan out selects for
             * arrange/assert (see {@code doesNotFlagLoopSelectInTestSource}), so the N+1 gate
             * must not fire there. Short-circuit on any source file whose {@link JavaSourceSet}
             * marker name is not {@code "main"}; a file with no marker at all is treated as
             * non-main (excluded), matching the conservative default (mirrors
             * {@link NoCachedTestEntitySourceField}'s test-source guard, inverted).
             */
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
     *
     * <p>Only counts as "enclosing" when the select is reached through the loop's <em>body</em>
     * — a select in the loop's header/control (a for-each iterable, a for-loop init/condition/
     * update, a while/do-while condition) runs once per loop entry, not once per iteration, so it
     * must not be flagged. {@code for (Foo f : repo.getByEntityNameSet(names)) {...}} — iterating
     * a batched result — is the motivating clean case. A select reached via a header keeps
     * ascending past that loop, since an outer loop's body may still enclose it. A select in a
     * while/do-while condition is deliberately left unflagged as a fail-safe under-flag (covered
     * by the runtime select-count gate) rather than special-cased.
     */
    static @Nullable String enclosingIterationKind(Cursor siteCursor) {
        Object child = siteCursor.getValue();
        Cursor cursor = siteCursor.getParent();
        while (cursor != null) {
            Object value = cursor.getValue();
            String kind = loopBodyKind(value, child);
            if (kind != null) {
                return kind;
            }
            if (value instanceof J.MethodDeclaration || value instanceof J.ClassDeclaration) {
                return null;
            }
            child = value;
            cursor = cursor.getParent();
        }
        return null;
    }

    /**
     * Compares {@code child} against the loop's <em>padded</em> body ({@code
     * l.getPadding().getBody()}, a {@code JRightPadded<Statement>}), not {@code l.getBody()}. The
     * visitor pushes a cursor frame for that padding wrapper itself (see {@code
     * JavaVisitor#visitRightPadded}) before descending into the unwrapped body statement, so the
     * cursor value directly above a loop is the {@code JRightPadded} wrapper — comparing against
     * the unwrapped {@code Statement} would never match and silently under-flag every loop body.
     */
    private static @Nullable String loopBodyKind(Object node, Object child) {
        if (node instanceof J.ForEachLoop l && l.getPadding().getBody() == child) {
            return "for-each-loop";
        }
        if (node instanceof J.ForLoop l && l.getPadding().getBody() == child) {
            return "for-loop";
        }
        if (node instanceof J.WhileLoop l && l.getPadding().getBody() == child) {
            return "while-loop";
        }
        if (node instanceof J.DoWhileLoop l && l.getPadding().getBody() == child) {
            return "do-while-loop";
        }
        return null;
    }
}
