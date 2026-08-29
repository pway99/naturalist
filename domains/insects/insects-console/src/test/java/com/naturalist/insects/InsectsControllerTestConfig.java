package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.resilience.Resilience;
import com.naturalist.usage.IdentificationBudget;
import gg.jte.TemplateEngine;
import gg.jte.springframework.boot.autoconfigure.JteProperties;
import gg.jte.springframework.boot.autoconfigure.JteViewResolver;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.ViewResolver;

/**
 * Spring Boot test composition for {@link InsectsController} slice tests
 * ({@code @WebMvcTest}). Wires the controller and its render pipeline from the
 * in-memory test contexts — no {@code DomainServiceScan}, no database, and
 * deliberately no Spring Security.
 *
 * <p>The controller reads the signed-in naturalist from the
 * {@code "naturalist.currentNaturalistName"} request attribute (see
 * {@link InsectsController}), so slice tests set that attribute directly rather
 * than standing up a security filter chain. Collaborators the exercised
 * endpoints never touch — {@code VisionService}, {@code ExternalAuthority} — are
 * passed as {@code null}, exactly as the direct-construction unit tests
 * ({@code InsectsControllerOverBudgetTest}) already do.
 *
 * <p>Full-page rendering uses the same filesystem-backed JTE engine the
 * template tests use ({@link TestTemplateEngine}: app layout dir + this module's
 * jte dir), wrapped in gg.jte's Spring {@link JteViewResolver} — the identical
 * resolver the app wires via the jte-spring-boot starter. This is a
 * {@code @SpringBootConfiguration} so {@code @WebMvcTest} discovers it as the
 * context anchor.
 */
@SpringBootConfiguration
class InsectsControllerTestConfig {

    /**
     * The slice's single in-memory database, exposed as a {@code static} so the slice
     * test can {@code @RegisterExtension} this very instance (JUnit then resets it per
     * test) while the beans below wire from it. It is a {@link NaturalistTestExtension}
     * — a {@link com.naturalist.data.NaturalistDatabase} subtype — obtained via
     * {@code NaturalistTestExtension.create()}, the sanctioned test-DB acquisition; a
     * bare {@code NaturalistDatabase.create()} in test sources is rejected by the
     * {@code AcquireDatabaseViaExtension} enforcement recipe. The in-memory sources
     * lazy-load their JSON fixtures on first access.
     */
    static final NaturalistTestExtension DATABASE = NaturalistTestExtension.create();

    @Bean
    InsectsController insectsController() {
        InsectsTestContext insects = InsectsTestContext.create(DATABASE);
        LibraryTestContext library = LibraryTestContext.create(DATABASE);
        return new InsectsController(
                Resilience.noOp(),
                null,                       // VisionService -- unused by the exercised endpoints
                insects.insectQuery(),
                insects.insectCommand(),
                insects.insectLifeStageQuery(),
                library.cladeQuery(),
                library.glossaryTermQuery(),
                library.libraryCommand(),
                null,                       // ExternalAuthority -- unused
                IdentificationBudget.noOp());
    }

    @Bean
    TemplateEngine jteTemplateEngine() {
        return TestTemplateEngine.create();
    }

    @Bean
    ViewResolver jteViewResolver(TemplateEngine engine) {
        JteProperties properties = new JteProperties();
        properties.setTemplateSuffix(".jte");
        return new JteViewResolver(engine, properties);
    }
}
