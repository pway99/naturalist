package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.observation.test.ImageFixtures;
import com.naturalist.resilience.Resilience;
import com.naturalist.usage.IdentificationBudget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Only an invalid <em>upload</em> maps to {@code ?error=image}. An
 * {@link InvariantViolationException} raised deeper in the identification pipeline (here: by
 * the vision stage, with a perfectly valid photo) is a server fault and must surface as an
 * error, not be mislabelled as a bad photo.
 */
class InsectsControllerPipelineInvariantTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @TempDir
    Path imageRoot;

    private static final byte[] JPEG_BYTES = ImageFixtures.decodableJpeg();

    @Test
    void identify_pipelineInvariantViolation_isNotMappedToImageError() throws IOException {
        FilesystemImageStore imageStore = new FilesystemImageStore(imageRoot, Resilience.noOp());
        InsectsTestContext context = InsectsTestContext.create(nte, imageStore, Resilience.noOp(),
                IdentificationBudget.noOp(),
                (image, tool, prompt) -> {
                    throw new InvariantViolationException("vision.pipeline", Set.of());
                },
                InsectsTestContext.noOpAuthority());
        LibraryTestContext library = LibraryTestContext.create(nte);
        InsectsController controller = new InsectsController(
                context.insectQuery(), context.insectViewQuery(), context.insectCommand(),
                library.cladeQuery(), library.glossaryTermQuery(), imageStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("naturalist.currentNaturalistName", "pat-way");
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);

        assertThatThrownBy(() -> controller.identify(image, null, null, null, null, null, request))
                .isInstanceOf(InvariantViolationException.class);

        try (Stream<Path> walk = Files.walk(imageRoot)) {
            assertThat(walk.filter(Files::isRegularFile)).isEmpty();
        }
    }
}
