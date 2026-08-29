package com.naturalist.spring.console;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateNotFoundException;
import gg.jte.resolve.DirectoryCodeResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Builds a filesystem-backed JTE {@link TemplateEngine} spanning every console's
 * templates, for use in console tests — template-render tests and
 * {@code @WebMvcTest} slices alike. Mirrors the app's runtime {@code JteConfiguration}:
 * it discovers {@code apps/<app>/src/main/jte} (the shared layout) and
 * {@code domains/<domain>/<domain>-console/src/main/jte} (each domain's templates)
 * from the project source tree, so a domain template that extends the app layout
 * resolves both. App-shell roots come first, so a layout template is never
 * shadowed by a domain of the same name. Templates compile on the fly — no
 * precompiled classes required.
 *
 * <p>Replaces the per-console {@code TestTemplateEngine} copies: a console test
 * calls {@link #create()} instead of maintaining its own resolver.
 */
public final class ConsoleSliceTemplates {

    private ConsoleSliceTemplates() {
    }

    public static TemplateEngine create() {
        Path projectRoot = findProjectRoot();
        List<DirectoryCodeResolver> resolvers = new ArrayList<>();
        addJteRootsUnder(projectRoot.resolve("apps"), resolvers);
        Path domainsDir = projectRoot.resolve("domains");
        if (Files.isDirectory(domainsDir)) {
            for (Path domain : childDirectories(domainsDir)) {
                addJteRootsUnder(domain, resolvers);
            }
        }
        return TemplateEngine.create(new CompositeCodeResolver(List.copyOf(resolvers)), ContentType.Html);
    }

    private static void addJteRootsUnder(Path parent, List<DirectoryCodeResolver> resolvers) {
        if (!Files.isDirectory(parent)) {
            return;
        }
        for (Path child : childDirectories(parent)) {
            Path jte = child.resolve("src/main/jte");
            if (Files.isDirectory(jte)) {
                resolvers.add(new DirectoryCodeResolver(jte));
            }
        }
    }

    private static List<Path> childDirectories(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path findProjectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve("pom.xml")) && Files.isDirectory(dir.resolve("kernels"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("Could not locate project root from " + Path.of("").toAbsolutePath());
    }

    private record CompositeCodeResolver(List<DirectoryCodeResolver> resolvers) implements CodeResolver {

        @Override
        public String resolve(String name) {
            for (DirectoryCodeResolver r : resolvers) {
                String result = r.resolve(name);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        @Override
        public String resolveRequired(String name) throws TemplateNotFoundException {
            String result = resolve(name);
            if (result == null) {
                throw new TemplateNotFoundException("Template not found: " + name);
            }
            return result;
        }

        @Override
        public boolean exists(String name) {
            return resolvers.stream().anyMatch(r -> r.exists(name));
        }

        @Override
        public long getLastModified(String name) {
            for (DirectoryCodeResolver r : resolvers) {
                if (r.exists(name)) {
                    return r.getLastModified(name);
                }
            }
            return 0;
        }

        @Override
        public List<String> resolveAllTemplateNames() {
            List<String> names = new ArrayList<>();
            for (DirectoryCodeResolver r : resolvers) {
                names.addAll(r.resolveAllTemplateNames());
            }
            return names;
        }
    }
}
