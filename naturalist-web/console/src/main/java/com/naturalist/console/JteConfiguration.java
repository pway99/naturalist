package com.naturalist.console;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateNotFoundException;
import gg.jte.resolve.DirectoryCodeResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Configuration
class JteConfiguration {

    private static final String[] TEMPLATE_ROOTS = {
            "naturalist-web/console/src/main/jte",
            "domains/chemistry/chemistry-console/src/main/jte",
            "domains/insects/insects-console/src/main/jte",
            "domains/plants/plants-console/src/main/jte",
    };

    @Bean
    TemplateEngine jteTemplateEngine() {
        var projectRoot = findProjectRoot();
        var resolvers = new ArrayList<DirectoryCodeResolver>();
        for (var root : TEMPLATE_ROOTS) {
            var path = projectRoot.resolve(root);
            if (Files.isDirectory(path)) {
                resolvers.add(new DirectoryCodeResolver(path));
            }
        }
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
