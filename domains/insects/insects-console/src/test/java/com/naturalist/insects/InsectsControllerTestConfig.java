package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.resilience.Resilience;
import com.naturalist.spring.console.ConsoleSliceRenderConfiguration;
import com.naturalist.usage.IdentificationBudget;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot test composition for {@link InsectsController} slice tests
 * ({@code @WebMvcTest}). Wires the controller from the in-memory test contexts —
 * no {@code DomainServiceScan}, no database, and deliberately no Spring Security.
 * Full-page JTE rendering comes from the shared
 * {@link ConsoleSliceRenderConfiguration} (app layout + this module's templates),
 * so this class carries only the domain-specific wiring.
 *
 * <p>The controller reads the signed-in naturalist from the
 * {@code "naturalist.currentNaturalistName"} request attribute (see
 * {@link InsectsController}), so slice tests set that attribute directly rather
 * than standing up a security filter chain. Collaborators the exercised
 * endpoints never touch — {@code VisionService}, {@code ExternalAuthority} — are
 * passed as {@code null}, exactly as the direct-construction unit tests
 * ({@code InsectsControllerOverBudgetTest}) already do.
 *
 * <p>This is a {@code @SpringBootConfiguration} so {@code @WebMvcTest} discovers
 * it as the context anchor.
 */
@SpringBootConfiguration
@Import(ConsoleSliceRenderConfiguration.class)
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
}
