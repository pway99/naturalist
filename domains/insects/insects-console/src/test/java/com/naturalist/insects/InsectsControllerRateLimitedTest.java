package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Bulkhead;
import com.naturalist.resilience.CircuitBreaker;
import com.naturalist.resilience.RateLimitExceededException;
import com.naturalist.resilience.RateLimiter;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Retry;
import com.naturalist.resilience.Timeout;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.vision.NoOpVisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InsectsController#identify} must turn an
 * {@link com.naturalist.identification.IdentificationRefused} with reason
 * {@code RATE_LIMITED} -- raised by {@code insectCommand.identification()}
 * ({@link InsectIdentificationCommandImpl#identify}) when its {@code vision.identification}
 * rate limiter rejects a permit, <em>before</em> the budget reservation, the image store and
 * any vision call -- into the same kind of friendly redirect as
 * {@link InsectsControllerOverBudgetTest}'s budget-exceeded case -- this console has
 * no flash-attribute / {@code @ControllerAdvice} infrastructure, so the over-rate
 * signal travels the same way: a query parameter on the {@code redirect:} target.
 * Per the design decision, rate limiting has no {@code usage.LimitKind} member --
 * {@code ?limit=RATE} is a UI-only copy branch on {@code identify.jte}, not an enum
 * value.
 *
 * <p>The gate no longer lives in the controller: {@link InsectIdentificationCommandImpl}
 * resolves {@code resilience.rateLimiter(InsectIdentificationCommandImpl.RATE_STRATEGY)} in
 * its constructor, and {@link InsectsTestContext#create(com.naturalist.data.NaturalistDatabase,
 * com.naturalist.observation.ImageStore, Resilience, IdentificationBudget,
 * com.naturalist.vision.VisionService, com.naturalist.authority.ExternalAuthority)} wires it
 * from the {@link Resilience} given there. This test drives the rejection by supplying a
 * {@link Resilience} whose {@code vision.identification} rate limiter always throws
 * {@link RateLimitExceededException} -- exactly the kernel exception the production
 * {@code adapters/resilience-resilience4j} bridge translates its vendor rejection into
 * (see {@code Resilience4jRateLimiterTest}), never the vendor type itself; the command maps
 * it to {@code IdentificationRefused(RATE_LIMITED)}. The budget reservation happens strictly
 * after the rate gate, so {@link IdentificationBudget#noOp()} lets every other collaborator
 * stay a real, un-exercised implementation from {@link InsectsTestContext} /
 * {@link LibraryTestContext} -- {@link NoOpVisionService} is never reached either.
 */
class InsectsControllerRateLimitedTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    LibraryTestContext libraryContext = LibraryTestContext.create(nte);

    // Minimal valid JPEG (magic bytes, padded to the 12 bytes ImageUpload sniffs) --
    // a valid upload, so the refusal can only come from the rate gate.
    private static final byte[] JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03,
            0, 0, 0, 0, 0
    };

    @TempDir
    Path imageRoot;

    /**
     * A {@link Resilience} that mirrors {@link Resilience#noOp()} for every
     * primitive except {@code rateLimiter}, whose {@code execute} always throws
     * {@link RateLimitExceededException} -- the shape
     * {@code InsectIdentificationCommandImpl}'s rate gate observes when the
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

            @Override
            public RateLimiter forKey(String key) {
                return this;
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
        var imageStore = new FilesystemImageStore(imageRoot, Resilience.noOp());
        var context = InsectsTestContext.create(nte, imageStore,
                new RejectingRateLimiterResilience(),
                IdentificationBudget.noOp(),
                new NoOpVisionService(),      // never called; the rate gate throws first
                InsectsTestContext.noOpAuthority()); // never called; the rate gate throws first
        return new InsectsController(
                context.insectQuery(),
                context.insectViewQuery(),
                context.insectCommand(),
                libraryContext.cladeQuery(),
                libraryContext.glossaryTermQuery(),
                imageStore);
    }

    @Test
    void identify_redirectsWithLimitRate_whenRateLimiterRejects() throws IOException {
        var request = new MockHttpServletRequest();
        request.setAttribute("naturalist.currentNaturalistName", "pat-way");
        var image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);

        String view = controller().identify(image, null, null, null, null, null, request);

        assertThat(view).isEqualTo("redirect:/insects/identify?limit=RATE");
    }
}
