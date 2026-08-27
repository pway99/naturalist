package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.resilience.Bulkhead;
import com.naturalist.resilience.CircuitBreaker;
import com.naturalist.resilience.RateLimitExceededException;
import com.naturalist.resilience.RateLimiter;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Retry;
import com.naturalist.resilience.Timeout;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.vision.VisionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InsectsController#identify} must turn a {@link RateLimitExceededException}
 * thrown by the {@code vision.identification} rate limiter (acquired by
 * {@link InsectIdentificationCommand#identify} <em>before</em> the budget
 * reservation and any vision call) into the same kind of friendly redirect as
 * {@link InsectsControllerOverBudgetTest}'s budget-exceeded case -- this console has
 * no flash-attribute / {@code @ControllerAdvice} infrastructure, so the over-rate
 * signal travels the same way: a query parameter on the {@code redirect:} target.
 * Per the design decision, rate limiting has no {@code usage.LimitKind} member --
 * {@code ?limit=RATE} is a UI-only copy branch on {@code identify.jte}, not an enum
 * value.
 *
 * <p>{@link InsectsController} builds its own {@link InsectIdentificationCommand}
 * internally from the {@link Resilience} it is constructed with (see
 * {@code resilience.rateLimiter("vision.identification")} at the composition
 * site), so this test drives the rejection by supplying a {@link Resilience} whose
 * {@code vision.identification} rate limiter always throws
 * {@link RateLimitExceededException} -- exactly the kernel exception the production
 * {@code adapters/resilience-resilience4j} bridge translates its vendor rejection
 * into (see {@code Resilience4jRateLimiterTest}), never the vendor type itself. The
 * budget reservation happens strictly after the rate gate
 * ({@link InsectIdentificationCommand#identify}'s step 0a before 0b), so
 * {@link IdentificationBudget#noOp()} lets every other collaborator stay a real,
 * un-exercised implementation from {@link InsectsTestContext} /
 * {@link LibraryTestContext} -- {@link VisionService} is never reached either.
 */
class InsectsControllerRateLimitedTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectsTestContext context = InsectsTestContext.create(nte);
    LibraryTestContext libraryContext = LibraryTestContext.create(nte);

    // Minimal valid JPEG magic bytes -- ImageStorageService.store validates
    // content type before the rate gate fires.
    private static final byte[] JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03
    };

    /**
     * A {@link Resilience} that mirrors {@link Resilience#noOp()} for every
     * primitive except {@code rateLimiter}, whose {@code execute} always throws
     * {@link RateLimitExceededException} -- the shape
     * {@code InsectIdentificationCommand}'s rate gate observes when the
     * {@code vision.identification} strategy rejects a permit.
     */
    private static final class RejectingRateLimiterResilience implements Resilience {

        private static final RateLimiter REJECTING = new RateLimiter() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                throw new RateLimitExceededException(
                        "Rate limit exceeded for strategy 'vision.identification'",
                        "vision.identification");
            }

            @Override
            public void execute(Runnable runnable) {
                execute(() -> {
                    runnable.run();
                    return null;
                });
            }
        };

        @Override
        public Retry retry(String name) {
            return Resilience.noOp().retry(name);
        }

        @Override
        public Timeout timeout(String name) {
            return Resilience.noOp().timeout(name);
        }

        @Override
        public CircuitBreaker circuitBreaker(String name) {
            return Resilience.noOp().circuitBreaker(name);
        }

        @Override
        public Bulkhead bulkhead(String name) {
            return Resilience.noOp().bulkhead(name);
        }

        @Override
        public RateLimiter rateLimiter(String name) {
            return REJECTING;
        }

        @Override
        public Set<String> retryNames() {
            return Set.of();
        }

        @Override
        public Set<String> timeoutNames() {
            return Set.of();
        }

        @Override
        public Set<String> circuitBreakerNames() {
            return Set.of();
        }

        @Override
        public Set<String> bulkheadNames() {
            return Set.of();
        }

        @Override
        public Set<String> rateLimiterNames() {
            return Set.of("vision.identification");
        }
    }

    private InsectsController controller() {
        return new InsectsController(
                new RejectingRateLimiterResilience(),
                null, // VisionService -- never called; the rate gate throws first
                context.insectQuery(),
                context.insectCommand(),
                context.insectLifeStageQuery(),
                libraryContext.cladeQuery(),
                libraryContext.glossaryTermQuery(),
                libraryContext.libraryCommand(),
                null, // ExternalAuthority -- never called; the rate gate throws first
                IdentificationBudget.noOp());
    }

    @Test
    void identify_redirectsWithLimitRate_whenRateLimiterRejects() throws IOException {
        var request = new MockHttpServletRequest();
        request.setAttribute("naturalist.currentNaturalistName", "pat-way");
        var image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);

        String view = controller().identify(image, null, null, null, request);

        assertThat(view).isEqualTo("redirect:/insects/identify?limit=RATE");
    }

    // InsectsController hard-codes its image storage directory to
    // "data/images/insects" relative to the working directory (not injectable);
    // identify() writes the uploaded image to disk before the rate gate fires,
    // so this test cleans up the one file (and now-empty directories) it creates
    // rather than leaving stray untracked files in the worktree.
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
