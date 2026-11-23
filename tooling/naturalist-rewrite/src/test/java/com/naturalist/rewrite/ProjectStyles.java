package com.naturalist.rewrite;

import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.style.NamedStyles;

import java.util.List;

/**
 * The styles the root pom activates for the rewrite gate, in its order: IntelliJ's, then the project style. A
 * test of the {@code EnforceArchitecture} composite parses under them, as the gate does, because the composite
 * formats every source it reads.
 */
final class ProjectStyles {

    static final List<NamedStyles> ACTIVE = Environment.builder().scanRuntimeClasspath().build()
            .activateStyles("org.openrewrite.java.IntelliJ", "com.naturalist.Style");

    private ProjectStyles() {
    }

    /**
     * The gate's parser, with {@code stubs} (stand-ins for library and kernel types) on its classpath: they are
     * types the subject compiles against, not sources the composite formats.
     */
    static JavaParser.Builder<?, ?> parser(String... stubs) {
        return JavaParser.fromJavaVersion().styles(ACTIVE).dependsOn(stubs);
    }
}
