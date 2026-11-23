package com.naturalist.rewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

/**
 * Removes a {@code @JsonCreator} that a named value in main source declares itself. The supertypes
 * ({@code EntityName}, {@code EntityId}, {@code NamedValue}) carry a class-level {@code @JsonDeserialize} that
 * reads every subtype through {@code NamedValues}, and Jackson prefers it to the subtype's creator, so the
 * subtype's annotation is dead and only misleads a reader about how the value is read.
 *
 * <p>Test sources are left alone: a fixture may keep a creator on purpose, to show the supertype wins.
 */
public class NoSubtypeJsonCreator extends Recipe {

    private static final String JSON_CREATOR = "com.fasterxml.jackson.annotation.JsonCreator";

    private static final List<String> SUPERTYPES = List.of(
            "com.naturalist.ddd.EntityName", "com.naturalist.ddd.EntityId", "com.naturalist.ddd.NamedValue");

    @Override
    public String getDisplayName() {
        return "A named value does not declare its own @JsonCreator";
    }

    @Override
    public String getDescription() {
        return "Removes @JsonCreator from every EntityName, NamedValue (numeric included) and EntityId in main "
                + "source: the supertypes' deserializer reads them through NamedValues, so the subtype's creator is "
                + "never called.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher creator = new AnnotationMatcher("@" + JSON_CREATOR);
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
                J.ClassDeclaration owner = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (owner == null
                        || !isNamedValue(owner.getType())
                        || md.getLeadingAnnotations().stream().noneMatch(creator::matches)) {
                    return md;
                }
                Cursor parent = getCursor().getParentOrThrow();
                md = (J.MethodDeclaration) new RemoveAnnotationVisitor(creator).visitNonNull(md, ctx, parent);
                maybeRemoveImport(JSON_CREATOR);
                return md;
            }
        };
    }

    private static boolean isNamedValue(JavaType.@Nullable FullyQualified type) {
        return type != null && SUPERTYPES.stream().anyMatch(s -> TypeUtils.isAssignableTo(s, type));
    }
}
