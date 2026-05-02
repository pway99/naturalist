package com.naturalist.console;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateNotFoundException;
import gg.jte.resolve.DirectoryCodeResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Wires the JTE template engine. Template roots are discovered from the
 * project layout — adding a new domain-console module requires no
 * configuration change here, only the standard
 * {@code <module>/src/main/jte} directory and a dependency entry in this
 * module's {@code pom.xml}.
 *
 * <p>Two predictable parents are scanned:
 * <ul>
 *   <li>{@code apps/*}/src/main/jte — the application shell and any other
 *       deployment artifact colocated under apps/.</li>
 *   <li>{@code domains/<domain>/<domain>-console}/src/main/jte — every
 *       domain console module follows this path.</li>
 * </ul>
 */
@Configuration
class JteConfiguration {

    @Bean
    TemplateEngine jteTemplateEngine() {
        var projectRoot = findProjectRoot();
        var roots = discoverTemplateRoots(projectRoot);
        var resolvers = roots.stream()
                .map(DirectoryCodeResolver::new)
                .toList();
        var codeResolver = new CompositeCodeResolver(resolvers);
        return TemplateEngine.create(codeResolver, projectRoot.resolve("jte-classes"), ContentType.Html, getClass().getClassLoader());
    }

    private Path findProjectRoot() {
        var dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve("pom.xml")) && Files.isDirectory(dir.resolve("kernels"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("Could not locate project root from " + Path.of("").toAbsolutePath());
    }

    /**
     * Walk the predictable console-host parents — {@code apps/*} and
     * {@code domains/<domain>/*} — and collect every existing
     * {@code <module>/src/main/jte} directory. Order is stable: app shell
     * roots first (so the layout templates resolve before any domain
     * shadows them), then domain consoles in directory iteration order.
     */
    private List<Path> discoverTemplateRoots(Path projectRoot) {
        var roots = new ArrayList<Path>();
        addJteRootsUnder(projectRoot.resolve("apps"), roots);
        var domainsDir = projectRoot.resolve("domains");
        if (Files.isDirectory(domainsDir)) {
            for (var domain : list(domainsDir)) {
                addJteRootsUnder(domain, roots);
            }
        }
        return roots;
    }

    private void addJteRootsUnder(Path parent, List<Path> roots) {
        if (!Files.isDirectory(parent)) return;
        for (var child : list(parent)) {
            var jte = child.resolve("src/main/jte");
            if (Files.isDirectory(jte)) {
                roots.add(jte);
            }
        }
    }

    private static List<Path> list(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record CompositeCodeResolver(List<DirectoryCodeResolver> resolvers) implements CodeResolver {

        @Override
        public String resolve(String name) {
            for (var resolver : resolvers) {
                var result = resolver.resolve(name);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        @Override
        public String resolveRequired(String name) throws TemplateNotFoundException {
            for (var resolver : resolvers) {
                var result = resolver.resolve(name);
                if (result != null) {
                    return result;
                }
            }
            throw new TemplateNotFoundException("Template not found: " + name);
        }

        @Override
        public boolean exists(String name) {
            return resolvers.stream().anyMatch(r -> r.exists(name));
        }

        @Override
        public long getLastModified(String name) {
            for (var resolver : resolvers) {
                if (resolver.exists(name)) {
                    return resolver.getLastModified(name);
                }
            }
            return 0;
        }

        @Override
        public List<String> resolveAllTemplateNames() {
            var names = new ArrayList<String>();
            for (var resolver : resolvers) {
                names.addAll(resolver.resolveAllTemplateNames());
            }
            return names;
        }
    }
}
