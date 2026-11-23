package com.naturalist.rewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

/**
 * Replaces every {@code var} declaration (a local, a for-each variable, a try-with-resources resource, a lambda
 * parameter) with the type the compiler inferred, importing what it names: explicitly, under the project style
 * {@code com.naturalist.Style}, which never folds imports into a star. A type whose simple name another import
 * already takes is written fully qualified. A type that has no name in source (an anonymous class, an
 * intersection, a capture with no bound) is marked, not rewritten: the author names the supertype it is used as.
 *
 * <p>Applies to main and test sources alike: the code base spells its types out.
 */
public class NoVar extends Recipe {

    static final String UNSPELLABLE = "var hides a type with no name in source: declare the variable as its supertype "
            + "or interface";

    @Override
    public String getDisplayName() {
        return "Spell out the type of every var";
    }

    @Override
    public String getDescription() {
        return "Replaces `var` with the declared variable's inferred type, adding the imports it needs.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);
                TypeTree typeExpression = mv.getTypeExpression();
                if (!(typeExpression instanceof J.Identifier id) || !"var".equals(id.getSimpleName())
                        || mv.getVariables().size() != 1) {
                    return mv;
                }
                JavaType type = mv.getVariables().getFirst().getType();
                JavaSourceFile file = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
                Map<String, String> imported = importedSimpleNames(file);
                Set<String> imports = new LinkedHashSet<>();
                String spelled = spell(type, imported, packageOf(file), imports);
                if (spelled == null) {
                    return SearchResult.found(mv, UNSPELLABLE);
                }
                // spelled here, so referenced: AddImport does not count a type argument as a reference
                imports.forEach(fqn -> maybeAddImport(fqn, null, false));
                J.Identifier explicit = new J.Identifier(randomId(), id.getPrefix(), Markers.EMPTY,
                        Collections.emptyList(), spelled, type, null);
                return mv.withTypeExpression(explicit);
            }
        };
    }

    private static String packageOf(JavaSourceFile file) {
        return file.getPackageDeclaration() == null ? "" : file.getPackageDeclaration().getPackageName();
    }

    /** Simple name → the fully qualified name its single-type import binds. */
    private static Map<String, String> importedSimpleNames(JavaSourceFile file) {
        Map<String, String> names = new HashMap<>();
        for (J.Import anImport : file.getImports()) {
            if (!anImport.isStatic() && !"*".equals(anImport.getQualid().getSimpleName())) {
                names.put(anImport.getQualid().getSimpleName(), anImport.getTypeName().replace('$', '.'));
            }
        }
        return names;
    }

    /** The source spelling of {@code type}, or null when it has none. Adds each top-level class to import. */
    private static @Nullable String spell(@Nullable JavaType type, Map<String, String> imported, String pkg, Set<String> imports) {
        if (type instanceof JavaType.Primitive primitive) {
            return primitive == JavaType.Primitive.None || primitive == JavaType.Primitive.Null
                    || primitive == JavaType.Primitive.Void ? null : primitive.getKeyword();
        }
        if (type instanceof JavaType.Array array) {
            String element = spell(array.getElemType(), imported, pkg, imports);
            return element == null ? null : element + "[]";
        }
        if (type instanceof JavaType.Parameterized parameterized) {
            String raw = spellClass(parameterized.getType(), imported, pkg, imports);
            if (raw == null) {
                return null;
            }
            if (parameterized.getTypeParameters().isEmpty()) {
                return raw;
            }
            List<String> arguments = parameterized.getTypeParameters().stream()
                    .map(t -> spellArgument(t, imported, pkg, imports))
                    .toList();
            return arguments.contains(null) ? null : raw + "<" + String.join(", ", arguments) + ">";
        }
        if (type instanceof JavaType.GenericTypeVariable variable) {
            // a type variable in scope (T); a capture or wildcard has no name at the top level
            return "?".equals(variable.getName()) || variable.getName().isEmpty() ? null : variable.getName();
        }
        if (type instanceof JavaType.FullyQualified fq) {
            return spellClass(fq, imported, pkg, imports);
        }
        return null;
    }

    private static @Nullable String spellArgument(JavaType type, Map<String, String> imported, String pkg, Set<String> imports) {
        if (type instanceof JavaType.GenericTypeVariable variable && "?".equals(variable.getName())) {
            if (variable.getBounds().isEmpty()) {
                return "?";
            }
            String bounds = variable.getBounds().stream()
                    .map(b -> spell(b, imported, pkg, imports))
                    .collect(Collectors.collectingAndThen(Collectors.toList(),
                            l -> l.contains(null) ? null : String.join(" & ", l)));
            if (bounds == null) {
                return null;
            }
            return variable.getVariance() == JavaType.GenericTypeVariable.Variance.CONTRAVARIANT
                    ? "? super " + bounds : "? extends " + bounds;
        }
        return spell(type, imported, pkg, imports);
    }

    /** A class's spelling: {@code Outer.Inner} with {@code Outer} imported, or fully qualified on a name clash. */
    private static @Nullable String spellClass(JavaType.FullyQualified fq, Map<String, String> imported, String pkg, Set<String> imports) {
        if (fq instanceof JavaType.Unknown || fq.getFullyQualifiedName().isEmpty()) {
            return null;
        }
        if (fq instanceof JavaType.Parameterized parameterized) {
            return spell(parameterized, imported, pkg, imports);
        }
        JavaType.FullyQualified outermost = fq;
        StringBuilder nested = new StringBuilder();
        while (outermost.getOwningClass() != null) {
            nested.insert(0, "." + outermost.getClassName().substring(outermost.getClassName().lastIndexOf('.') + 1));
            outermost = outermost.getOwningClass();
        }
        String top = outermost.getFullyQualifiedName();
        String simple = top.substring(top.lastIndexOf('.') + 1);
        if (simple.isEmpty() || Character.isDigit(simple.charAt(0)) || top.matches(".*\\$\\d.*")) {
            return null; // anonymous or local class
        }
        String topPackage = outermost.getPackageName();
        String bound = imported.get(simple);
        boolean javaLang = "java.lang".equals(topPackage);
        boolean samePackage = topPackage.equals(pkg);
        if (bound != null && !bound.equals(top)) {
            return top + nested; // the simple name is taken by another import
        }
        if (!javaLang && !samePackage && bound == null) {
            imports.add(top);
        }
        return simple + nested;
    }
}
