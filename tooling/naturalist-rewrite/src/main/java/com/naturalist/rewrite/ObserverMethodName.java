package com.naturalist.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

/**
 * Marks an {@code Observer} observation whose method name is not the method it runs in. The name scopes the
 * {@code naturalist.invariant.check} metric ({@code Class.method.constraint}); a method renamed without its
 * observation goes on counting under a method that no longer exists. {@code forMethod}, {@code arguments},
 * {@code state} and {@code noticeError} take the name of the nearest enclosing method (a lambda is part of the
 * method it is written in; an anonymous class's method is a method of its own), and {@code "constructor"} in a
 * constructor, a record's compact one included. A name that is not a string literal is not checked, and neither
 * is {@code MethodObserver.noticeError}, whose first argument names the failure, not the method.
 *
 * <p>Marks, never rewrites: renaming an observation renames its metric, so the author decides what dashboards
 * and alerts follow. Applies to main and test sources alike (a test observes under its own method name).
 */
public class ObserverMethodName extends Recipe {

    private static final String OBSERVER = "com.naturalist.observability.Observer";

    private static final Set<String> NAMED_BY_METHOD = Set.of("forMethod", "arguments", "state", "noticeError");

    static String message(String observed, String expected) {
        return "observes as \"" + observed + "\" inside " + expected + ": an observation names the method it runs in";
    }

    @Override
    public String getDisplayName() {
        return "Observations name the method they run in";
    }

    @Override
    public String getDescription() {
        return "Marks an `Observer` `forMethod` / `arguments` / `state` / `noticeError` call whose method name is "
                + "not the enclosing method's (or `constructor`).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(invocation, ctx);
                JavaType.Method type = mi.getMethodType();
                if (type == null || !NAMED_BY_METHOD.contains(mi.getSimpleName())
                        || !TypeUtils.isOfClassType(type.getDeclaringType(), OBSERVER)
                        || mi.getArguments().isEmpty()) {
                    return mi;
                }
                Expression first = mi.getArguments().getFirst();
                if (!(first instanceof J.Literal literal) || !(literal.getValue() instanceof String observed)) {
                    return mi;
                }
                J.MethodDeclaration enclosing = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (enclosing == null) {
                    return mi; // a field initializer or initializer block: no method to name
                }
                String expected = enclosing.isConstructor() || enclosing.getReturnTypeExpression() == null
                        ? "constructor" : enclosing.getSimpleName();
                return observed.equals(expected) ? mi : SearchResult.found(mi, message(observed, expected));
            }
        };
    }
}
