package com.naturalist.rewrite;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

/**
 * Fails the dry run on any {@code System.getenv} outside {@code com.naturalist.secrets}
 * (ADR-028): deployment secrets reach code only through {@code SecretProvider}, and the
 * environment source is the one place that reads the process environment.
 */
public class NoSystemGetenv extends Recipe {
    public static final String MESSAGE = "read the process environment outside com.naturalist.secrets; resolve the value through SecretProvider (ADR-028)";

    private static final String SECRETS_PACKAGE = "com.naturalist.secrets";

    /**
     * RdbmsDataSource reads NATURALIST_TEST_JDBC_URL, _DB_USER and _DB_PASSWORD: the
     * standing-test-database contract in ci.yml, not a deployed secret.
     */
    private static final Set<String> EXEMPT_CLASSES = Set.of("com.naturalist.persistence.test.RdbmsDataSource");

    private static final MethodMatcher MATCHER = new MethodMatcher("java.lang.System getenv(..)");

    @Override
    public String getDisplayName() {
        return "Read the process environment only through SecretProvider";
    }

    @Override
    public String getDescription() {
        return "System.getenv is allowed only in com.naturalist.secrets, where the environment source "
             + "lives, and in the standing-test-database wiring RdbmsDataSource (a CI contract, not a "
             + "deployed secret). Everything else resolves secrets through SecretProvider (ADR-028).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(MATCHER), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                if (!MATCHER.matches(mi)) {
                    return mi;
                }
                J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
                String pkg = cu == null || cu.getPackageDeclaration() == null
                        ? "" : cu.getPackageDeclaration().getPackageName();
                if (pkg.equals(SECRETS_PACKAGE) || pkg.startsWith(SECRETS_PACKAGE + ".")) {
                    return mi;
                }
                J.ClassDeclaration outermost = null;
                for (Cursor c = getCursor(); c != null; c = c.getParent()) {
                    if (c.getValue() instanceof J.ClassDeclaration cd) {
                        outermost = cd;
                    }
                }
                if (outermost != null) {
                    String fqn = pkg.isEmpty() ? outermost.getSimpleName() : pkg + "." + outermost.getSimpleName();
                    if (EXEMPT_CLASSES.contains(fqn)) {
                        return mi;
                    }
                }
                return SearchResult.found(mi, MESSAGE);
            }
        });
    }
}
