package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Resilience;
import com.naturalist.usage.BudgetExceededException;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.usage.LimitKind;
import com.naturalist.vision.NoOpVisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InsectsController#identify} must turn the
 * {@link com.naturalist.identification.IdentificationRefused} (reason
 * {@code BUDGET_EXHAUSTED}, detail the {@link LimitKind} name) that
 * {@code insectCommand.identification()} ({@link InsectIdentificationCommandImpl#identify})
 * raises when its {@link IdentificationBudget} throws {@link BudgetExceededException} into a
 * friendly redirect rather than a raw 500 —
 * this console has no flash-attribute / {@code @ControllerAdvice} infrastructure,
 * so the over-budget signal travels the same way every other message on this
 * page does: a query parameter on the {@code redirect:} target
 * (see {@code identify.jte}'s existing {@code identified} param).
 *
 * <p>The budget gate no longer lives in the controller: it is the command's second step
 * (after the rate gate, before the image store, vision, authority, or any DB write), wired
 * from the {@link IdentificationBudget} handed to
 * {@link InsectsTestContext#create(com.naturalist.data.NaturalistDatabase,
 * com.naturalist.observation.ImageStore, Resilience, IdentificationBudget,
 * com.naturalist.vision.VisionService, com.naturalist.authority.ExternalAuthority)}. A
 * throwing {@link IdentificationBudget} therefore lets every other collaborator stay a real,
 * un-exercised implementation from {@link InsectsTestContext} /
 * {@link LibraryTestContext} — nothing else needs stubbing.
 */
class InsectsControllerOverBudgetTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    LibraryTestContext libraryContext = LibraryTestContext.create(nte);

    // Minimal valid JPEG (magic bytes, padded to the 12 bytes ImageUpload sniffs) --
    // a valid upload, so the refusal can only come from the budget.
    private static final byte[] JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03,
            0, 0, 0, 0, 0
    };

    @TempDir
    Path imageRoot;

    private static final IdentificationBudget EXCEEDED_BUDGET = naturalist -> {
        throw new BudgetExceededException(LimitKind.MONTHLY, Instant.parse("2026-09-01T00:00:00Z"));
    };

    private InsectsController controller() {
        var imageStore = new FilesystemImageStore(imageRoot, Resilience.noOp());
        var context = InsectsTestContext.create(nte, imageStore,
                Resilience.noOp(),
                EXCEEDED_BUDGET,
                new NoOpVisionService(),      // never called; budget.reserve() throws first
                InsectsTestContext.noOpAuthority()); // never called; budget.reserve() throws first
        return new InsectsController(
                context.insectQuery(),
                context.insectViewQuery(),
                context.insectCommand(),
                libraryContext.cladeQuery(),
                libraryContext.glossaryTermQuery(),
                imageStore);
    }

    @Test
    void identify_redirectsWithLimitParam_whenBudgetExceeded() throws IOException {
        var request = new MockHttpServletRequest();
        request.setAttribute("naturalist.currentNaturalistName", "pat-way");
        var image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);

        String view = controller().identify(image, null, null, null, request);

        assertThat(view).isEqualTo("redirect:/insects/identify?limit=MONTHLY");
    }

}
