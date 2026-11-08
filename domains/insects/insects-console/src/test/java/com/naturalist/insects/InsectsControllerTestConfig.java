package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Resilience;
import com.naturalist.spring.console.ConsoleSliceRenderConfiguration;
import com.naturalist.test.TestDirectories;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

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
 * than standing up a security filter chain. The identification collaborators
 * ({@code VisionService}, {@code ExternalAuthority}, rate gate, budget) live behind
 * {@code insectCommand().identification()} and take the test context's no-op defaults.
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

    /** A per-JVM temp directory the slice's {@link #IMAGE_STORE} writes uploads under. */
    static final Path IMAGE_ROOT = createImageRoot();

    /**
     * The controller's {@link com.naturalist.observation.ImageStore}, exposed so a slice test
     * can store a photo directly and then request it through the serve endpoint.
     */
    static final FilesystemImageStore IMAGE_STORE = new FilesystemImageStore(IMAGE_ROOT, Resilience.noOp());

    /**
     * The insects context the controller bean is built from, exposed so slice tests can read
     * state back through {@code INSECTS.insectQuery()} after driving the controller.
     */
    static final InsectsTestContext INSECTS = InsectsTestContext.create(DATABASE, IMAGE_STORE);

    private static Path createImageRoot() {
        try {
            return TestDirectories.create("insects-images");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Bean
    InsectsController insectsController() {
        LibraryTestContext library = LibraryTestContext.create(DATABASE);
        return new InsectsController(
                INSECTS.insectQuery(),
                INSECTS.insectViewQuery(),
                INSECTS.insectCommand(),
                library.cladeQuery(),
                library.glossaryTermQuery(),
                IMAGE_STORE);
    }
}
