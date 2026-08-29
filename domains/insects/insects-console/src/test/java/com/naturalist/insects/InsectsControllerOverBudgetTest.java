package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.resilience.Resilience;
import com.naturalist.usage.BudgetExceededException;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.usage.LimitKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InsectsController#identify} must turn a {@link BudgetExceededException}
 * from {@link InsectIdentificationCommand#identify} (via the injected
 * {@link IdentificationBudget}) into a friendly redirect rather than a raw 500 —
 * this console has no flash-attribute / {@code @ControllerAdvice} infrastructure,
 * so the over-budget signal travels the same way every other message on this
 * page does: a query parameter on the {@code redirect:} target
 * (see {@code identify.jte}'s existing {@code identified} param).
 *
 * <p>The budget reservation is the very first thing {@code identify(...)} does
 * (before vision, authority, or any DB write), so a throwing
 * {@link IdentificationBudget} lets every other collaborator stay a real,
 * un-exercised implementation from {@link InsectsTestContext} /
 * {@link LibraryTestContext} — nothing else needs stubbing.
 */
class InsectsControllerOverBudgetTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectsTestContext context = InsectsTestContext.create(nte);
    LibraryTestContext libraryContext = LibraryTestContext.create(nte);

    // Minimal valid JPEG magic bytes -- ImageStorageService.store validates
    // content type before the budget reservation happens.
    private static final byte[] JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03
    };

    private static final IdentificationBudget EXCEEDED_BUDGET = naturalist -> {
        throw new BudgetExceededException(LimitKind.MONTHLY, Instant.parse("2026-09-01T00:00:00Z"));
    };

    private InsectsController controller() {
        return new InsectsController(
                Resilience.noOp(),
                null, // VisionService -- never called; budget.reserve() throws first
                context.insectQuery(),
                context.insectCommand(),
                context.insectLifeStageQuery(),
                libraryContext.cladeQuery(),
                libraryContext.glossaryTermQuery(),
                libraryContext.libraryCommand(),
                null, // ExternalAuthority -- never called; budget.reserve() throws first
                EXCEEDED_BUDGET);
    }

    @Test
    void identify_redirectsWithLimitParam_whenBudgetExceeded() throws IOException {
        var request = new MockHttpServletRequest();
        request.setAttribute("naturalist.currentNaturalistName", "pat-way");
        var image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);

        String view = controller().identify(image, null, null, null, request);

        assertThat(view).isEqualTo("redirect:/insects/identify?limit=MONTHLY");
    }

    // InsectsController hard-codes its image storage directory to
    // "data/images/insects" relative to the working directory (not injectable);
    // identify() writes the uploaded image to disk (under a generated UUID name)
    // before the budget check fires. Rather than nuke the shared directory —
    // which other slice tests in this module write into concurrently across the
    // suite — snapshot its contents before the test and delete only the files
    // this test added, leaving the directory and every other test's files intact.
    private static final Path IMAGE_DIR = Path.of("data/images/insects");
    private Set<Path> imagesBefore;

    @BeforeEach
    void snapshotStoredImages() throws IOException {
        imagesBefore = storedImages();
    }

    @AfterEach
    void cleanUpStoredImage() throws IOException {
        for (Path stored : storedImages()) {
            if (!imagesBefore.contains(stored)) {
                Files.deleteIfExists(stored);
            }
        }
    }

    private static Set<Path> storedImages() throws IOException {
        if (!Files.isDirectory(IMAGE_DIR)) {
            return Set.of();
        }
        try (var paths = Files.list(IMAGE_DIR)) {
            return paths.collect(Collectors.toSet());
        }
    }
}
