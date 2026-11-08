package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Resilience;
import com.naturalist.test.TestDirectories;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

@SpringBootConfiguration
@Import(com.naturalist.spring.console.ConsoleSliceRenderConfiguration.class)
class PlantsControllerTestConfig {
    /** The slice's single in-memory database, registered by the slice test and wired into the beans below. */
    static final NaturalistTestExtension DATABASE = NaturalistTestExtension.create();

    /** A per-JVM temp directory the slice's {@link #IMAGE_STORE} writes under. */
    static final Path IMAGE_ROOT = createImageRoot();

    /** The controller's ImageStore, exposed so a slice test can store a photo and then request it. */
    static final FilesystemImageStore IMAGE_STORE = new FilesystemImageStore(IMAGE_ROOT, Resilience.noOp());

    private static Path createImageRoot() {
        try {
            return TestDirectories.create("plants-images");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Bean
    PlantsController plantsController() {
        PlantsTestContext plants = PlantsTestContext.create(DATABASE);
        LibraryTestContext library = LibraryTestContext.create(DATABASE);
        return new PlantsController(
                plants.plantQuery(),
                plants.cultivarQuery(),
                plants.seedLineageQuery(),
                plants.plantProgramQuery(),
                plants.phytochemicalConstituentQuery(),
                library.cladeQuery(),
                IMAGE_STORE);
    }
}
