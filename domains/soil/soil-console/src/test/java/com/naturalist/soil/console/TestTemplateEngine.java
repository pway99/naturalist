package com.naturalist.soil.console;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateNotFoundException;
import gg.jte.resolve.DirectoryCodeResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class TestTemplateEngine {

    private static final String[] TEMPLATE_ROOTS = {
            "apps/management-console/src/main/jte",
            "domains/soil/soil-console/src/main/jte",
    };

    private TestTemplateEngine() {
    }

    static TemplateEngine create() {
        Path projectRoot = findProjectRoot();
        List<DirectoryCodeResolver> resolvers = new ArrayList<>();
        for (String root : TEMPLATE_ROOTS) {
            Path path = projectRoot.resolve(root);
            if (Files.isDirectory(path)) {
                resolvers.add(new DirectoryCodeResolver(path));
            }
        }
        return TemplateEngine.create(new CompositeCodeResolver(resolvers), ContentType.Html);
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
