package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.resilience.Resilience;
import com.naturalist.usage.BudgetExceededException;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.usage.LimitKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;

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
    // identify() writes the uploaded image to disk before the budget check
    // fires, so this test cleans up the one file (and now-empty directories)
    // it creates rather than leaving stray untracked files in the worktree.
    @AfterEach
    void cleanUpStoredImage() throws IOException {
        var dataDir = Path.of("data");
        if (!Files.exists(dataDir)) {
            return;
        }
        try (var paths = Files.walk(dataDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
