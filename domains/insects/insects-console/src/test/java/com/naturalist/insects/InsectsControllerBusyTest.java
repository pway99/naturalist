package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.observation.test.ImageFixtures;
import com.naturalist.resilience.Bulkhead;
import com.naturalist.resilience.BulkheadFullException;
import com.naturalist.resilience.CircuitBreaker;
import com.naturalist.resilience.RateLimiter;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Retry;
import com.naturalist.resilience.Timeout;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.vision.NoOpVisionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
    private static final byte[] JPEG_BYTES = ImageFixtures.decodableJpeg();

    @TempDir
    Path imageRoot;

    SimpleMeterRegistry meters;

    @BeforeEach
    void addMeters() {
        meters = new SimpleMeterRegistry();
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void removeMeters() {
        Metrics.removeRegistry(meters);
    }

    /** The endpoint counted the full bulkhead once, as a failure it answered rather than threw. */
    private void assertBusyCounted() {
        Counter busy = meters.find("naturalist.invariant.check")
                .tag("name", "InsectsController.serve.read")
                .tag("kind", "caught")
                .tag("handling", "swallowed")
                .counter();
        assertThat(busy).as("busy count").isNotNull();
        assertThat(busy.count()).as("busy count").isEqualTo(1.0);
    }

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
        FilesystemImageStore imageStore = new FilesystemImageStore(imageRoot, Resilience.noOp());
        InsectsTestContext context = InsectsTestContext.create(nte, imageStore,
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
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("naturalist.currentNaturalistName", "pat-way");
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);

        assertThat(controller().identify(image, null, null, null, null, null, request))
                .isEqualTo("redirect:/insects/identify?limit=BUSY");
    }

    /**
     * {@link InsectsController#image} answers a full {@code image.store} bulkhead with an uncached 503
     * and {@code Retry-After}, never a 500 or a cached failure (PL-16).
     */
    @Test
    void image_isServiceUnavailable_whenTheBulkheadRejects() {
        FilesystemImageStore busyStore = new FilesystemImageStore(imageRoot, new RejectingBulkheadResilience());
        InsectsTestContext context = InsectsTestContext.create(nte, busyStore, Resilience.noOp(), IdentificationBudget.noOp(),
                new NoOpVisionService(), InsectsTestContext.noOpAuthority());
        InsectsController controller = new InsectsController(context.insectQuery(), context.insectViewQuery(),
                context.insectCommand(), libraryContext.cladeQuery(), libraryContext.glossaryTermQuery(), busyStore);

        ResponseEntity<Resource> response = controller.image(FileName.of("anything.jpg"));

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isNull();
        assertBusyCounted();
    }

    /**
     * {@link InsectsController#tile} answers a full {@code image.store} bulkhead with an uncached 503
     * and {@code Retry-After}, never a 500, a cached failure or a fallback to the full photo.
     */
    @Test
    void tile_isServiceUnavailable_whenTheBulkheadRejects() {
        FilesystemImageStore busyStore = new FilesystemImageStore(imageRoot, new RejectingBulkheadResilience());
        InsectsTestContext context = InsectsTestContext.create(nte, busyStore, Resilience.noOp(), IdentificationBudget.noOp(),
                new NoOpVisionService(), InsectsTestContext.noOpAuthority());
        InsectsController controller = new InsectsController(context.insectQuery(), context.insectViewQuery(),
                context.insectCommand(), libraryContext.cladeQuery(), libraryContext.glossaryTermQuery(), busyStore);

        ResponseEntity<Resource> response = controller.tile(FileName.of("anything.jpg"));

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isNull();
        assertBusyCounted();
    }

    /**
     * {@link InsectsController#addImage} answers a full {@code image.store.write} bulkhead with {@code ?error=busy} --
     * the species page has no busy message of its own, so this is a new one -- never a 500, and nothing is stored.
     */
    @Test
    void addImage_redirectsWithErrorBusy_whenTheStoreHasNoWritePermit() throws IOException {
        FilesystemImageStore busyStore = new FilesystemImageStore(imageRoot, new RejectingBulkheadResilience());
        InsectsTestContext context = InsectsTestContext.create(nte, busyStore, Resilience.noOp(), IdentificationBudget.noOp(),
                new NoOpVisionService(), InsectsTestContext.noOpAuthority());
        InsectsController controller = new InsectsController(context.insectQuery(), context.insectViewQuery(),
                context.insectCommand(), libraryContext.cladeQuery(), libraryContext.glossaryTermQuery(), busyStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("naturalist.currentNaturalistName", "pat-way");
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);

        assertThat(controller.addImage(InsectSpeciesName.of("battus-philenor"), image, null, null, null, null, request))
                .isEqualTo("redirect:/insects/battus-philenor?error=busy");
        try (Stream<Path> files = Files.walk(imageRoot)) {
            assertThat(files.filter(Files::isRegularFile).toList()).as("nothing is stored").isEmpty();
        }
    }

    /**
     * A full {@code image.store.write} bulkhead on identify is the same busy refusal as a full vision bulkhead:
     * {@code ?limit=BUSY}, and the budget is never charged.
     */
    @Test
    void identify_redirectsWithLimitBusy_whenTheStoreHasNoWritePermit() throws IOException {
        FilesystemImageStore busyStore = new FilesystemImageStore(imageRoot, new RejectingBulkheadResilience());
        AtomicInteger reservations = new AtomicInteger();
        IdentificationBudget counting = naturalist -> reservations.incrementAndGet();
        InsectsTestContext context = InsectsTestContext.create(nte, busyStore, Resilience.noOp(), counting,
                new NoOpVisionService(), InsectsTestContext.noOpAuthority());
        InsectsController controller = new InsectsController(context.insectQuery(), context.insectViewQuery(),
                context.insectCommand(), libraryContext.cladeQuery(), libraryContext.glossaryTermQuery(), busyStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("naturalist.currentNaturalistName", "pat-way");
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);

        assertThat(controller.identify(image, null, null, null, null, null, request))
                .isEqualTo("redirect:/insects/identify?limit=BUSY");
        assertThat(reservations.get()).as("the budget is never charged").isZero();
    }
}
