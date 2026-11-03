package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Bulkhead;
import com.naturalist.resilience.BulkheadFullException;
import com.naturalist.resilience.CircuitBreaker;
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
 * {@link com.naturalist.identification.IdentificationRefused} with reason {@code BUSY} --
 * raised by {@link InsectIdentificationCommandImpl#identify} when the
 * {@code vision.identification} bulkhead has no free permit -- into a friendly redirect,
 * the same way {@link InsectsControllerRateLimitedTest} handles {@code RATE_LIMITED}:
 * {@code ?limit=BUSY} is a UI-only copy branch on {@code identify.jte}.
 *
 * <p>The {@link Resilience} here rejects on {@code bulkhead} with the kernel
 * {@link BulkheadFullException} -- the type the production Resilience4j bridge translates
 * its vendor rejection into -- and passes the rate gate through, so the refusal can only
 * come from the bulkhead. {@link NoOpVisionService} is never reached.
 */
class InsectsControllerBusyTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    LibraryTestContext libraryContext = LibraryTestContext.create(nte);

    // Minimal valid JPEG (magic bytes, padded to the 12 bytes ImageUpload sniffs) --
    // a valid upload, so the refusal can only come from the bulkhead.
    private static final byte[] JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03,
            0, 0, 0, 0, 0
    };

    @TempDir
    Path imageRoot;

    /**
     * A {@link Resilience} that mirrors {@link Resilience#noOp()} for every primitive except
     * {@code bulkhead}, whose {@code execute} always throws {@link BulkheadFullException}.
     */
    private static final class RejectingBulkheadResilience implements Resilience {

        private static final Bulkhead REJECTING = new Bulkhead() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                throw new BulkheadFullException(
                        "Bulkhead full for strategy 'vision.identification'",
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
            return REJECTING;
        }

        @Override
        public RateLimiter rateLimiter(String name) {
            return Resilience.noOp().rateLimiter(name);
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
            return Set.of("vision.identification");
        }

        @Override
        public Set<String> rateLimiterNames() {
            return Set.of();
        }
    }

    private InsectsController controller() {
        var imageStore = new FilesystemImageStore(imageRoot, Resilience.noOp());
        var context = InsectsTestContext.create(nte, imageStore,
                new RejectingBulkheadResilience(),
                IdentificationBudget.noOp(),
                new NoOpVisionService(),      // never called; the bulkhead throws first
                InsectsTestContext.noOpAuthority()); // never called; the bulkhead throws first
        return new InsectsController(
                context.insectQuery(),
                context.insectViewQuery(),
                context.insectCommand(),
                libraryContext.cladeQuery(),
                libraryContext.glossaryTermQuery(),
                imageStore);
    }

    @Test
    void identify_redirectsWithLimitBusy_whenTheBulkheadRejects() throws IOException {
        var request = new MockHttpServletRequest();
        request.setAttribute("naturalist.currentNaturalistName", "pat-way");
        var image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);

        assertThat(controller().identify(image, null, null, null, request))
                .isEqualTo("redirect:/insects/identify?limit=BUSY");
    }
}
