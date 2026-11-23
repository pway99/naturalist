package com.naturalist.rewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Marks a web handler parameter that should take a named value's type instead of {@code String}, and an id or
 * numeric named-value parameter that is still required.
 *
 * <ul>
 *   <li>{@link #TYPED}: a {@code @PathVariable} / {@code @RequestParam String p} that the handler reads straight
 *       into a named value ({@code XName.of(p)}, {@code XId.of(UUID.fromString(p))}, {@code X.of(new BigDecimal(p))}).
 *       Declared as that type, Spring converts it through {@code NamedValueConversion}, the strategy JSON reads use.</li>
 *   <li>{@link #REQUIRED}: a parameter typed as an {@code EntityId} or {@code NumericNamedValue} that is neither
 *       {@code required = false} nor {@code Optional}. Its text may not read ({@code null}), and Spring refuses a
 *       {@code null} conversion of a required parameter itself, an uncounted 400, before the handler can observe
 *       it. A {@code String}-backed name never converts to {@code null}, so it may stay required.</li>
 * </ul>
 *
 * <p>Handler methods (a Spring {@code @*Mapping}) in main source only. Report-only until every controller is
 * converted; then it joins {@code EnforceArchitecture}.
 */
public class TypedPathVariable extends Recipe {
    public static final String TYPED = "a handler parameter read straight into a named value takes that type: Spring converts it through NamedValues";
    public static final String REQUIRED = "an id or numeric named-value handler parameter is required = false or Optional: Spring refuses a required one whose text does not read, an uncounted 400";

    private static final String WEB = "org.springframework.web.bind.annotation.";
    private static final Set<String> MAPPINGS = Set.of(
            WEB + "RequestMapping", WEB + "GetMapping", WEB + "PostMapping",
            WEB + "PutMapping", WEB + "DeleteMapping", WEB + "PatchMapping");
    private static final Set<String> BINDINGS = Set.of(WEB + "PathVariable", WEB + "RequestParam");

    private static final List<String> NAMED_VALUES = List.of(
            "com.naturalist.ddd.EntityName", "com.naturalist.ddd.EntityId", "com.naturalist.ddd.NamedValue");
    private static final List<String> MAY_READ_NULL = List.of(
            "com.naturalist.ddd.EntityId", "com.naturalist.ddd.NumericNamedValue");

    @Override
    public String getDisplayName() {
        return "A handler takes a named value's type, and an id or number is not required";
    }

    @Override
    public String getDescription() {
        return "Marks a @PathVariable / @RequestParam String a Spring handler reads straight into a named value "
             + "(it takes the named value's type instead), and an EntityId or NumericNamedValue handler parameter "
             + "that is neither required = false nor Optional.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile file) {
                    boolean isMain = file.getMarkers()
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
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                if (md.getLeadingAnnotations().stream().noneMatch(a -> isAny(a, MAPPINGS))) {
                    return md;
                }
                J.Block body = md.getBody();
                return md.withParameters(ListUtils.map(md.getParameters(), p -> {
                    if (!(p instanceof J.VariableDeclarations param)) {
                        return p;
                    }
                    J.@Nullable Annotation binding = param.getLeadingAnnotations().stream()
                            .filter(a -> isAny(a, BINDINGS)).findFirst().orElse(null);
                    if (binding == null) {
                        return p;
                    }
                    JavaType type = param.getType();
                    if (TypeUtils.isString(type)) {
                        String name = param.getVariables().getFirst().getSimpleName();
                        return body != null && readIntoNamedValue(body, name) ? SearchResult.found(param, TYPED) : p;
                    }
                    if (MAY_READ_NULL.stream().anyMatch(s -> TypeUtils.isAssignableTo(s, type))
                            && !notRequired(binding)) {
                        return SearchResult.found(param, REQUIRED);
                    }
                    return p;
                }));
            }
        };
    }

    private static boolean isAny(J.Annotation annotation, Set<String> fqns) {
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
        return type != null && fqns.contains(type.getFullyQualifiedName());
    }

    /** {@code required = false} on the binding annotation. ({@code Optional} parameters never reach the type check.) */
    private static boolean notRequired(J.Annotation binding) {
        List<Expression> arguments = binding.getArguments();
        return arguments != null && arguments.stream().anyMatch(a -> a instanceof J.Assignment assignment
                && assignment.getVariable() instanceof J.Identifier id
                && id.getSimpleName().equals("required")
                && assignment.getAssignment() instanceof J.Literal literal
                && Boolean.FALSE.equals(literal.getValue()));
    }

    /** The body calls a named value's static {@code of} on {@code name}, directly or through one parse call. */
    private static boolean readIntoNamedValue(J.Block body, String name) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, AtomicBoolean hit) {
                J.MethodInvocation mi = super.visitMethodInvocation(invocation, hit);
                JavaType.Method type = mi.getMethodType();
                if (mi.getSimpleName().equals("of")
                        && type != null
                        && type.hasFlags(org.openrewrite.java.tree.Flag.Static)
                        && mi.getArguments().size() == 1
                        && NAMED_VALUES.stream().anyMatch(s -> TypeUtils.isAssignableTo(s, type.getDeclaringType()))
                        && isParameter(unwrap(mi.getArguments().getFirst()), name)) {
                    hit.set(true);
                }
                return mi;
            }
        }.visit(body, found);
        return found.get();
    }

    /** {@code UUID.fromString(p)} or {@code new BigDecimal(p)} → {@code p}; anything else as is. */
    private static Expression unwrap(Expression argument) {
        if (argument instanceof J.MethodInvocation call && call.getArguments().size() == 1) {
            return call.getArguments().getFirst();
        }
        if (argument instanceof J.NewClass created && created.getArguments().size() == 1) {
            return created.getArguments().getFirst();
        }
        return argument;
    }

    private static boolean isParameter(Expression expression, String name) {
        return expression instanceof J.Identifier id
                && id.getSimpleName().equals(name)
                && id.getFieldType() != null
                && id.getFieldType().getOwner() instanceof JavaType.Method;
    }
}
