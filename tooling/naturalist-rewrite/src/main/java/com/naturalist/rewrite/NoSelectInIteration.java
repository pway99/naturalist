package com.naturalist.rewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.Cursor;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.List;
import java.util.Set;

public class NoSelectInIteration extends Recipe {

    static final String MESSAGE =
        "N+1 fan-out: repository or query select invoked inside an iteration construct; "
        + "call the batched sibling once instead";

    private static final String REPOSITORY = "com.naturalist.data.EntityRepository";
    private static final String QUERY = "com.naturalist.data.EntityQuery";
    private static final Set<String> WRITES = Set.of("insert", "update", "save");

    /**
     * Pagination cursors: {@code getPage}/{@code findPage} are designed to be called once per
     * page inside a loop that walks a paged result set (see {@code EntityRepositoryTest}'s
     * page-walk contract test). This is not a per-element N+1 fan-out — there is no batched
     * sibling to call instead, since the loop itself IS the batching mechanism (one select per
     * page, not one select per already-collected element) — so these are excluded from
     * select-site detection.
     */
    private static final Set<String> PAGING = Set.of("getPage", "findPage");

    private final transient Findings findings = new Findings(this);

    public static class Findings extends DataTable<Findings.Row> {
        public Findings(Recipe recipe) {
            super(recipe,
                "N+1 select findings",
                "Repository or query selects invoked inside an iteration construct.");
        }

        public record Row(
            @Column(displayName = "Source path",
                    description = "Path of the file containing the finding.") String sourcePath,
            @Column(displayName = "Enclosing type",
                    description = "Simple name of the class holding the finding.") String enclosingType,
            @Column(displayName = "Enclosing method",
                    description = "Name of the method holding the finding.") String enclosingMethod,
            @Column(displayName = "Select",
                    description = "The repository/query method invoked per element.") String select,
            @Column(displayName = "Iteration kind",
                    description = "The loop or stream operation that fans the select out.") String iterationKind,
            @Column(displayName = "Suggested batched sibling",
                    description = "The batched method to call once instead.") String suggestedBatchedSibling) {
        }
    }

    /**
     * Per-element stream fan-out ops: any {@code Stream}/{@code IntStream}/{@code LongStream}/
     * {@code DoubleStream} instance method, plus {@code Iterable.forEach} and {@code Map.forEach}.
     * A select passed as a lambda/method-reference argument to one of these runs once per element
     * — the same N+1 shape as a loop body — so it is flagged the same way.
     */
    private static final List<MethodMatcher> FAN_OUT = List.of(
        new MethodMatcher("java.util.stream.Stream *(..)"),
        new MethodMatcher("java.util.stream.IntStream *(..)"),
        new MethodMatcher("java.util.stream.LongStream *(..)"),
        new MethodMatcher("java.util.stream.DoubleStream *(..)"),
        new MethodMatcher("java.lang.Iterable forEach(..)", true),
        new MethodMatcher("java.util.Map forEach(..)", true)
    );

    private static final String STREAM = "java.util.stream.BaseStream";

    /**
     * Labels a matched fan-out invocation for the data table: {@code "stream:" + name} when the
     * invocation's declaring type is a {@code BaseStream} (Stream/IntStream/LongStream/
     * DoubleStream), otherwise {@code "iterable:" + name} for Iterable/Map.forEach — which are
     * collection operations, not stream operations, and must not be mislabeled as "stream:...".
     */
    private static String fanOutKind(J.MethodInvocation fan) {
        JavaType.Method methodType = fan.getMethodType();
        JavaType.FullyQualified declaring = methodType == null ? null : methodType.getDeclaringType();
        String prefix = declaring != null && TypeUtils.isAssignableTo(STREAM, declaring)
            ? "stream:" : "iterable:";
        return prefix + fan.getSimpleName();
    }

    private static boolean matchesFanOut(Expression e) {
        return e instanceof J.MethodInvocation mi && FAN_OUT.stream().anyMatch(m -> m.matches(mi));
    }

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
                return recordAndMark(m, kind, m.getMethodType(), ctx);
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference mr, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(mr, ctx);
                if (!isSelectSite(m.getMethodType())) {
                    return m;
                }
                Cursor parent = nearestEnclosingInvocation(getCursor());
                if (parent != null && parent.getValue() instanceof J.MethodInvocation fan
                        && matchesFanOut(fan)) {
                    return recordAndMark(m, fanOutKind(fan), m.getMethodType(), ctx);
                }
                return m;
            }

            private <T extends J> T recordAndMark(T site, String kind, JavaType.Method methodType,
                                                    ExecutionContext ctx) {
                findings.insertRow(ctx, new Findings.Row(
                    sourcePath(), enclosingType(), enclosingMethod(),
                    methodType.getName(), kind, suggestedSibling(methodType)));
                return SearchResult.found(site, MESSAGE);
            }

            private String suggestedSibling(JavaType.Method methodType) {
                JavaType.FullyQualified declaring = methodType.getDeclaringType();
                if (declaring != null && TypeUtils.isAssignableTo(QUERY, declaring)) {
                    return "findByNameSet";
                }
                return "getByEntityNameSet";
            }

            private String enclosingType() {
                J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return c == null ? "" : c.getSimpleName();
            }

            private String enclosingMethod() {
                J.MethodDeclaration m = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return m == null ? "<initializer>" : m.getSimpleName();
            }

            private String sourcePath() {
                JavaSourceFile sf = getCursor().firstEnclosing(JavaSourceFile.class);
                return sf == null ? "" : sf.getSourcePath().toString();
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
        String name = methodType.getName();
        return isPort && !WRITES.contains(name) && !PAGING.contains(name);
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
            if (value instanceof J.Lambda) {
                Cursor parent = nearestEnclosingInvocation(cursor);
                if (parent != null && parent.getValue() instanceof J.MethodInvocation fan
                        && matchesFanOut(fan)) {
                    return fanOutKind(fan);
                }
                // a lambda that is not a per-element fan-out arg is a scope boundary: stop.
                return null;
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
     * A lambda or method reference passed as a call argument sits under {@code JRightPadded} (the
     * argument slot) and {@code JContainer} (the argument list) cursor frames before reaching the
     * enclosing {@code J.MethodInvocation} — the visitor pushes a cursor frame for each padding/
     * container wrapper itself (mirrors the {@code loopBodyKind} padding caveat below). Skip those
     * non-{@code J} wrapper frames to find the nearest actual syntax-tree ancestor.
     */
    private static @Nullable Cursor nearestEnclosingInvocation(Cursor cursor) {
        Cursor parent = cursor.getParent();
        while (parent != null && !(parent.getValue() instanceof J)) {
            parent = parent.getParent();
        }
        return parent;
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
