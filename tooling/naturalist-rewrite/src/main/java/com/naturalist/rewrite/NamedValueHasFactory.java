package com.naturalist.rewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.List;
import java.util.Set;

/**
 * Marks a concrete named value in main source that lacks the factory {@code NamedValues} reads it through: a
 * {@code public static} method named {@code of} with one parameter of type {@code String}, {@code BigDecimal}
 * or {@code UUID}, returning the type itself. Without it a catalog read or a path variable fails at runtime
 * instead of at build. A second such factory is marked too: NamedValues refuses a type with two. Other {@code of} overloads (composers such as {@code LifeStageName.of(species, kind)})
 * are allowed beside it.
 */
public class NamedValueHasFactory extends Recipe {
    public static final String MESSAGE = "a named value needs public static <Type> of(String | BigDecimal | UUID): NamedValues reads it through that factory";
    public static final String AMBIGUOUS = "a named value declares one public static of(String | BigDecimal | UUID); with two, NamedValues refuses it (factoryUnique)";

    private static final List<String> SUPERTYPES = List.of(
            "com.naturalist.ddd.EntityName", "com.naturalist.ddd.EntityId", "com.naturalist.ddd.NamedValue");

    private static final Set<String> VALUE_TYPES = Set.of("java.lang.String", "java.math.BigDecimal", "java.util.UUID");

    @Override
    public String getDisplayName() {
        return "A named value declares the factory NamedValues reads it through";
    }

    @Override
    public String getDescription() {
        return "Every concrete EntityName, NamedValue (numeric included) and EntityId in main source declares "
             + "public static <Type> of(String | BigDecimal | UUID): the one factory the supertypes' Jackson "
             + "deserializers and the Spring converter call through NamedValues.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            /** Main source only: a test fixture may declare a package-private factory, which NamedValues accepts. */
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
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                JavaType.FullyQualified type = cd.getType();
                if (type == null
                        || cd.getKind() == J.ClassDeclaration.Kind.Type.Interface
                        || cd.hasModifier(J.Modifier.Type.Abstract)
                        || SUPERTYPES.stream().noneMatch(s -> TypeUtils.isAssignableTo(s, type))) {
                    return cd;
                }
                long factories = factories(cd, type);
                return factories == 1 ? cd : SearchResult.found(cd, factories == 0 ? MESSAGE : AMBIGUOUS);
            }
        };
    }

    private static long factories(J.ClassDeclaration cd, JavaType.FullyQualified type) {
        long count = 0;
        for (Statement statement : cd.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration m
                    && m.getSimpleName().equals("of")
                    && m.hasModifier(J.Modifier.Type.Public)
                    && m.hasModifier(J.Modifier.Type.Static)
                    && m.getMethodType() != null
                    && m.getMethodType().getParameterTypes().size() == 1
                    && VALUE_TYPES.stream().anyMatch(v -> TypeUtils.isOfClassType(m.getMethodType().getParameterTypes().get(0), v))
                    && TypeUtils.isOfType(m.getMethodType().getReturnType(), type)) {
                count++;
            }
        }
        return count;
    }
}
