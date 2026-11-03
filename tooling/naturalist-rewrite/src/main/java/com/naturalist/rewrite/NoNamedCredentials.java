package com.naturalist.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.regex.Pattern;

/**
 * Fails the dry run on any Java source, main or test, whose text names a credential
 * (ADR-028): a vendor-and-role key name in any case or separator style, or a vendor key
 * prefix. A public repository that spells such names out is a target for key-scanning
 * searches. Secrets are referred to only through {@code SecretName} constants that do not
 * say what they unlock. The pattern below is written so this file does not match itself.
 */
public class NoNamedCredentials extends Recipe {
    public static final String MESSAGE = "names a credential; refer to a secret only through a SecretName that does not say what it unlocks (ADR-028)";

    private static final Pattern NAMED_CREDENTIAL = Pattern.compile("(?i)[a-z0-9]+[-_]api[-_]?key|sk-ant\\-");

    @Override
    public String getDisplayName() {
        return "Do not name credentials in source";
    }

    @Override
    public String getDescription() {
        return "Flags a source file whose literals, comments or identifiers spell a credential name "
             + "or a vendor key prefix. Name secrets through neutral SecretName constants (ADR-028).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if (NAMED_CREDENTIAL.matcher(cu.printAll()).find()) {
                    return SearchResult.found(cu, MESSAGE);
                }
                return cu;
            }
        };
    }
}
