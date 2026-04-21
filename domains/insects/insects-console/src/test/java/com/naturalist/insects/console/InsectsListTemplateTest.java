package com.naturalist.insects.console;

import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.InsectSpeciesTestEntitySource;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateNotFoundException;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Template-only regression tests for {@code insects/list.jte}.
 *
 * <p>Renders the template against a real {@link TemplateEngine} — no Spring, no
 * browser. Catches {@code @param} type drift, missing accessors, and template
 * compile errors the moment they happen, instead of waiting for the app to boot.
 */
class InsectsListTemplateTest {

    private static final String[] TEMPLATE_ROOTS = {
            "naturalist-web/console/src/main/jte",
            "domains/insects/insects-console/src/main/jte",
    };

    @Test
    void list_rendersSpeciesCatalog() {
        List<InsectSpecies> species = new InsectSpeciesTestEntitySource().entityStream().toList();
        StringOutput output = new StringOutput();

        templateEngine().render("insects/list.jte", Map.of("species", species), output);

        String html = output.toString();
        assertThat(html)
                .contains("Insect Catalog")
                .contains(species.size() + " species documented at Oak Vista.");
        for (InsectSpecies s : species) {
            assertThat(html).contains("/insects/" + s.name().value());
            assertThat(html).contains(s.taxonomy().order().value());
        }
    }

    private static TemplateEngine templateEngine() {
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
