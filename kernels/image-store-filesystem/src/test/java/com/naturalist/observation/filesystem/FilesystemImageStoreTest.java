package com.naturalist.observation.filesystem;

import com.naturalist.data.FileName;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Level;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.ImageVariant;
import com.naturalist.observation.StoredImage;
import com.naturalist.observation.test.ImageFixtures;
import com.naturalist.resilience.Bulkhead;
import com.naturalist.resilience.BulkheadFullException;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Metrics;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilesystemImageStoreTest {

    private static final byte[] JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4};

    /** A decodable JPEG, for what goes through {@code store}. */
    private static final byte[] PHOTO = image("jpg", 64, 48);

    private static final byte[] HEIC = {
            0, 0, 0, 0x18, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63, 0, 0, 0, 0};

    @TempDir
    Path root;

    FilesystemImageStore store;

    @BeforeEach
    void setUp() {
        store = new FilesystemImageStore(root, Resilience.noOp());
    }

    @Test
    void store_writesUnderSpaceWithSniffedExtension() {
        FileName name = store.store("insects", ImageUpload.of(PHOTO));

        assertThat(name.value()).endsWith(".jpg");
        assertThat(Files.exists(root.resolve("insects").resolve(name.value()))).isTrue();
    }

    @Test
    void store_createsSpaceDirectoryWhenMissing() throws IOException {
        store.store("insects", ImageUpload.of(PHOTO));
        Path space = root.resolve("insects");
        try (Stream<Path> tree = Files.walk(space)) {
            for (Path p : tree.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }

        FileName name = store.store("insects", ImageUpload.of(PHOTO));

        assertThat(Files.exists(space.resolve(name.value()))).isTrue();
    }

    @Test
    void store_rejectsInvalidUpload() throws IOException {
        assertThatThrownBy(() -> store.store("insects", ImageUpload.of(HEIC)))
                .isInstanceOf(InvariantViolationException.class);

        Path space = root.resolve("insects");
        if (Files.exists(space)) {
            try (Stream<Path> files = Files.list(space)) {
                assertThat(files.toList()).isEmpty();
            }
        }
    }

    @Test
    void read_roundTripsStoredBytesAndMediaType() {
        FileName name = store.store("insects", ImageUpload.of(PHOTO));

        Optional<StoredImage> read = store.read("insects", name);

        assertThat(read).isPresent();
        assertThat(read.get().readAllBytes()).isEqualTo(ImageUpload.of(PHOTO).withoutMetadata().bytes());
        assertThat(read.get().mediaType()).isEqualTo("image/jpeg");
        assertThat(read.get().size()).isEqualTo(ImageUpload.of(PHOTO).withoutMetadata().bytes().length);
    }

    /** A read finds the file; its content is opened only when streamed, and each open is fresh. */
    @Test
    void read_streamsContentOnEachOpen() throws IOException {
        FileName name = store.store("insects", ImageUpload.of(PHOTO));
        StoredImage stored = store.read("insects", name).orElseThrow();

        try (var first = stored.content().open(); var second = stored.content().open()) {
            byte[] expected = ImageUpload.of(PHOTO).withoutMetadata().bytes();
            assertThat(first.readAllBytes()).isEqualTo(expected);
            assertThat(second.readAllBytes()).isEqualTo(expected);
        }
    }

    @Test
    void store_stripsMetadata_readReturnsNone() {
        byte[] withGps = withApp1(PHOTO, ImageFixtures.app1Exif(ByteOrder.BIG_ENDIAN, 6, 45.5, -122.7));
        FileName name = store.store("insects", ImageUpload.of(withGps));

        StoredImage stored = store.read("insects", name).orElseThrow();

        ImageUpload upload = ImageUpload.of(stored.readAllBytes());
        assertThat(upload.gps()).isEmpty();
        assertThat(upload.withoutMetadata()).isEqualTo(upload);
    }

    @Test
    void store_refusesAnUploadThatStripsToANonImage() throws IOException {
        byte[] exif = ImageFixtures.app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7);
        byte[] bytes = new byte[2 + exif.length + 4];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        System.arraycopy(exif, 0, bytes, 2, exif.length);
        System.arraycopy(new byte[]{0x12, 0x34, 0x56, 0x78}, 0, bytes, 2 + exif.length, 4);
        ImageUpload upload = ImageUpload.of(bytes);
        assertThat(upload.isValid()).isTrue();

        assertThatThrownBy(() -> store.store("insects", upload)).isInstanceOf(InvariantViolationException.class);

        Path space = root.resolve("insects");
        if (Files.exists(space)) {
            try (Stream<Path> files = Files.list(space)) {
                assertThat(files.toList()).isEmpty();
            }
        }
    }

    @Test
    void read_missingIsEmpty() {
        assertThat(store.read("insects", FileName.of("nope.jpg"))).isEmpty();
    }

    @Test
    void read_rejectsPathTraversal() throws IOException {
        Files.writeString(root.resolve("secret.jpg"), "secret");
        Files.createDirectories(root.resolve("insects/sub"));
        Files.write(root.resolve("insects/sub/b.jpg"), JPEG);

        assertThat(store.read("insects", FileName.of("../../etc/passwd"))).isEmpty();
        assertThat(store.read("insects", FileName.of("../secret.jpg"))).isEmpty();
        assertThat(store.read("insects", FileName.of("sub/b.jpg"))).isEmpty();
        assertThat(store.read("insects", FileName.of("sub\\b.jpg"))).isEmpty();
    }

    @Test
    void read_unknownExtensionIsOctetStream() throws IOException {
        Files.createDirectories(root.resolve("insects"));
        Files.write(root.resolve("insects/blob.bin"), new byte[]{1, 2, 3});

        Optional<StoredImage> read = store.read("insects", FileName.of("blob.bin"));

        assertThat(read).isPresent();
        assertThat(read.get().mediaType()).isEqualTo("application/octet-stream");
    }

    @Test
    void read_sniffsMediaTypeFromBytes() throws IOException {
        Files.createDirectories(root.resolve("insects"));
        Files.write(root.resolve("insects/mislabelled.png"), JPEG);

        Optional<StoredImage> read = store.read("insects", FileName.of("mislabelled.png"));

        assertThat(read).isPresent();
        assertThat(read.get().mediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void delete_removesStoredFile() {
        FileName name = store.store("insects", ImageUpload.of(PHOTO));

        store.delete("insects", name);

        assertThat(Files.exists(root.resolve("insects").resolve(name.value()))).isFalse();
        assertThat(store.read("insects", name)).isEmpty();
    }

    @Test
    void delete_missingIsNoOp() {
        assertThatCode(() -> store.delete("insects", FileName.of("nope.jpg"))).doesNotThrowAnyException();
    }

    @Test
    void delete_rejectsPathTraversal() throws IOException {
        Path secret = root.resolve("secret.jpg");
        Files.writeString(secret, "secret");

        store.delete("insects", FileName.of("../secret.jpg"));

        assertThat(Files.exists(secret)).isTrue();
    }

    /** {@code testspace/images/seed.jpg} is on the test classpath but not in the root: photos are data, so it is not served. */
    @Test
    void read_ignoresAClasspathResource() {
        assertThat(store.read("testspace", FileName.of("seed.jpg"))).isEmpty();
    }

    /**
     * A read past the bulkhead cap is refused with the kernel {@link BulkheadFullException}, before any
     * file is touched, so the caller can answer 503.
     */
    @Test
    void read_propagatesTheBulkheadRefusal() {
        FileName name = store.store("insects", ImageUpload.of(PHOTO));
        var full = new FilesystemImageStore(root, rejectingBulkhead());

        assertThatThrownBy(() -> full.read("insects", name))
                .isInstanceOf(BulkheadFullException.class);
    }

    @Test
    void store_keepsAPhotoWithin1024ByteForByte() {
        byte[] bytes = image("jpg", 1024, 768);
        FileName name = store.store("insects", ImageUpload.of(bytes));

        assertThat(store.read("insects", name, ImageVariant.PHOTO).orElseThrow().readAllBytes())
                .isEqualTo(ImageUpload.of(bytes).withoutMetadata().bytes());
    }

    @Test
    void store_downscalesAnOversizedPhotoAndKeepsNoOriginal() throws IOException {
        FileName name = store.store("insects", ImageUpload.of(image("jpg", 3000, 2000)));

        BufferedImage photo = decode(store.read("insects", name).orElseThrow().readAllBytes());
        assertThat(photo.getWidth()).isEqualTo(1024);
        assertThat(photo.getHeight()).isEqualTo(683);
        try (Stream<Path> files = Files.list(root.resolve("insects"))) {
            List<Path> entries = files.toList();
            assertThat(entries.stream().filter(Files::isRegularFile)).hasSize(1);
            assertThat(entries.stream().filter(Files::isDirectory).map(p -> p.getFileName().toString()))
                    .containsExactly("tiles");
        }
    }

    @Test
    void store_writesA640Tile() throws IOException {
        FileName name = store.store("insects", ImageUpload.of(image("jpg", 3000, 2000)));

        StoredImage tile = store.read("insects", name, ImageVariant.TILE).orElseThrow();

        BufferedImage decoded = decode(tile.readAllBytes());
        assertThat(decoded.getWidth()).isEqualTo(640);
        assertThat(decoded.getHeight()).isEqualTo(427);
        assertThat(tile.mediaType()).isEqualTo("image/jpeg");
        assertThat(Files.isRegularFile(root.resolve("insects/tiles").resolve(name.value()))).isTrue();
    }

    @Test
    void store_namesAReencodedPngJpg() {
        FileName name = store.store("insects", ImageUpload.of(image("png", 2000, 2000)));

        assertThat(name.value()).endsWith(".jpg");
        assertThat(store.read("insects", name).orElseThrow().mediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void store_refusesAnUnstorableUploadAndWritesNothing() throws IOException {
        ImageUpload bomb = ImageUpload.of(pngDeclaring(20_000, 20_000));

        assertThatThrownBy(() -> store.store("insects", bomb)).isInstanceOf(InvariantViolationException.class);

        assertNothingWritten();
    }

    @Test
    void delete_removesPhotoAndTile() {
        FileName name = store.store("insects", ImageUpload.of(image("jpg", 3000, 2000)));

        store.delete("insects", name);

        assertThat(store.read("insects", name, ImageVariant.PHOTO)).isEmpty();
        assertThat(store.read("insects", name, ImageVariant.TILE)).isEmpty();
        assertThat(Files.exists(root.resolve("insects/tiles").resolve(name.value()))).isFalse();
    }

    @Test
    void read_missingTileIsEmpty() throws IOException {
        Files.createDirectories(root.resolve("insects"));
        Files.write(root.resolve("insects/seed.jpg"), PHOTO);

        assertThat(store.read("insects", FileName.of("seed.jpg"), ImageVariant.PHOTO)).isPresent();
        assertThat(store.read("insects", FileName.of("seed.jpg"), ImageVariant.TILE)).isEmpty();
    }

    @Test
    void requireStorable_refusesABombAndAcceptsAPhoto() {
        assertThatThrownBy(() -> store.requireStorable(ImageUpload.of(pngDeclaring(20_000, 20_000))))
                .isInstanceOf(InvariantViolationException.class);
        assertThatCode(() -> store.requireStorable(ImageUpload.of(image("jpg", 800, 600))))
                .doesNotThrowAnyException();
    }

    @Test
    void store_runsUnderTheWriteBulkhead() throws IOException {
        var full = new FilesystemImageStore(root, rejectingBulkhead(FilesystemImageStore.WRITE_STRATEGY));

        assertThatThrownBy(() -> full.store("insects", ImageUpload.of(image("jpg", 3000, 2000))))
                .isInstanceOf(BulkheadFullException.class);

        assertNothingWritten();
    }

    private void assertNothingWritten() throws IOException {
        Path space = root.resolve("insects");
        if (Files.exists(space)) {
            try (Stream<Path> files = Files.walk(space)) {
                assertThat(files.filter(Files::isRegularFile).toList()).isEmpty();
            }
        }
    }

    private static byte[] image(String format, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y += 8) {
            for (int x = 0; x < width; x += 8) {
                img.setRGB(x, y, (x * 31 + y * 17) & 0xFFFFFF);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /** {@code jpeg} with {@code app1} spliced in right after SOI. */
    private static byte[] withApp1(byte[] jpeg, byte[] app1) {
        byte[] out = new byte[jpeg.length + app1.length];
        System.arraycopy(jpeg, 0, out, 0, 2);
        System.arraycopy(app1, 0, out, 2, app1.length);
        System.arraycopy(jpeg, 2, out, 2 + app1.length, jpeg.length - 2);
        return out;
    }

    /** A PNG whose header declares {@code width} x {@code height} over a stub of data: it must never be decoded. */
    private static byte[] pngDeclaring(int width, int height) {
        ByteBuffer ihdr = ByteBuffer.allocate(13).putInt(width).putInt(height).put((byte) 8).put((byte) 2)
                .put((byte) 0).put((byte) 0).put((byte) 0);
        return ImageFixtures.png(ImageFixtures.chunk("IHDR", ihdr.array()), ImageFixtures.idat(), ImageFixtures.iend());
    }


    private static Resilience rejectingBulkhead() {
        return rejectingBulkhead(FilesystemImageStore.STRATEGY);
    }

    /** {@link Resilience#noOp()} for every primitive except the {@code bulkhead} of {@code strategy}, which always refuses. */
    private static Resilience rejectingBulkhead(String strategy) {
        Resilience noOp = Resilience.noOp();
        Bulkhead refusing = new Bulkhead() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                throw new BulkheadFullException("Bulkhead full for strategy '" + strategy + "'", strategy);
            }

            @Override
            public void execute(Runnable runnable) {
                execute(() -> {
                    runnable.run();
                    return null;
                });
            }
        };
        return (Resilience) Proxy.newProxyInstance(Resilience.class.getClassLoader(), new Class<?>[]{Resilience.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("bulkhead") && strategy.equals(args[0])) return refusing;
                    try {
                        return method.invoke(noOp, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /**
     * {@link Resilience#noOp()} for every primitive except the {@code timeout} of {@code strategy}, which behaves as the
     * real adapter does when it fires: the work goes to a worker thread that is never interrupted (here it waits for
     * {@code release}), and the caller gets an exception at once.
     */
    private static Resilience abandoningTimeout(String strategy, CountDownLatch release, List<Thread> workers) {
        Resilience noOp = Resilience.noOp();
        Timeout abandoning = new Timeout() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                Thread worker = new Thread(() -> {
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    supplier.get();
                });
                workers.add(worker);
                worker.start();
                throw new IllegalStateException("timed out");
            }

            @Override
            public void execute(Runnable runnable) {
                execute(() -> {
                    runnable.run();
                    return null;
                });
            }
        };
        return (Resilience) Proxy.newProxyInstance(Resilience.class.getClassLoader(), new Class<?>[]{Resilience.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("timeout") && strategy.equals(args[0])) return abandoning;
                    try {
                        return method.invoke(noOp, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /**
     * The timeout adapter never interrupts its worker: the caller gets the timeout while the worker has yet to encode
     * and write. Whichever finishes last must remove the files, so none is left once both are done.
     */
    @Test
    void store_leavesNothingWhenTheTimeoutAbandonsTheWorker() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        List<Thread> workers = new CopyOnWriteArrayList<>();
        var slow = new FilesystemImageStore(root, abandoningTimeout(FilesystemImageStore.WRITE_STRATEGY, release, workers));

        assertThatThrownBy(() -> slow.store("insects", ImageUpload.of(image("jpg", 3000, 2000))))
                .isInstanceOf(IllegalStateException.class);
        release.countDown();
        for (Thread worker : workers) {
            worker.join(10_000);
        }

        assertNothingWritten();
    }

    /** What {@code store} refuses after stripping, {@code requireStorable} refuses first, so the pre-check never passes an upload the store then rejects. */
    @Test
    void requireStorable_refusesAnUploadThatStripsToANonImage() {
        byte[] exif = ImageFixtures.app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7);
        byte[] bytes = new byte[2 + exif.length + 4];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        System.arraycopy(exif, 0, bytes, 2, exif.length);
        System.arraycopy(new byte[]{0x12, 0x34, 0x56, 0x78}, 0, bytes, 2 + exif.length, 4);
        ImageUpload upload = ImageUpload.of(bytes);
        assertThat(upload.isValid()).isTrue();

        assertThatThrownBy(() -> store.requireStorable(upload)).isInstanceOf(InvariantViolationException.class);
    }

    /** A space that resolves to the root or out of it is the caller's defect, refused through the Observer like any other argument. */
    @Test
    void store_refusesASpaceOutsideTheRootAndWritesNothing() throws Exception {
        ImageUpload upload = ImageUpload.of(image("jpg", 64, 48));

        assertThatThrownBy(() -> store.store("../escaped", upload)).isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> store.store(".", upload)).isInstanceOf(InvariantViolationException.class);

        assertThat(Files.exists(root.resolveSibling("escaped"))).isFalse();
        try (Stream<Path> files = Files.walk(root)) {
            assertThat(files.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }

    /** {@code bytes} written straight into {@code <root>/insects/name}, the way a seeder or an older store left a photo. */
    private Path placed(String name, byte[] bytes) throws IOException {
        Path file = root.resolve("insects").resolve(name);
        Files.createDirectories(file.getParent());
        return Files.write(file, bytes);
    }

    /** A photo over the edge is rewritten in place under its own name, and its tile is written beside it. */
    @Test
    void conform_downscalesInPlaceAndTiles() throws IOException {
        Path photo = placed("seed.jpg", image("jpg", 2048, 1536));

        FilesystemImageStore.ConformReport report = store.conform("insects");

        assertThat(report).isEqualTo(new FilesystemImageStore.ConformReport(1, 1, 0));
        BufferedImage rewritten = decode(Files.readAllBytes(photo));
        assertThat(rewritten.getWidth()).isEqualTo(1024);
        assertThat(rewritten.getHeight()).isEqualTo(768);
        BufferedImage tile = decode(Files.readAllBytes(root.resolve("insects/tiles/seed.jpg")));
        assertThat(tile.getWidth()).isEqualTo(640);
        assertThat(tile.getHeight()).isEqualTo(480);
        try (Stream<Path> files = Files.list(root.resolve("insects"))) {
            assertThat(files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())).containsExactly("seed.jpg");
        }
    }

    /** A photo already within the edge keeps its bytes; only its missing tile is written. */
    @Test
    void conform_tilesAPhotoWithinTheLimitWithoutTouchingIt() throws IOException {
        byte[] bytes = image("jpg", 1024, 768);
        Path photo = placed("seed.jpg", bytes);

        FilesystemImageStore.ConformReport report = store.conform("insects");

        assertThat(report).isEqualTo(new FilesystemImageStore.ConformReport(0, 1, 0));
        assertThat(Files.readAllBytes(photo)).isEqualTo(bytes);
        BufferedImage tile = decode(store.read("insects", FileName.of("seed.jpg"), ImageVariant.TILE).orElseThrow().readAllBytes());
        assertThat(tile.getWidth()).isEqualTo(640);
        assertThat(tile.getHeight()).isEqualTo(480);
    }

    /** Every regular file under {@code directory}, tiles included, with its bytes. */
    private static Map<Path, byte[]> contents(Path directory) throws IOException {
        Map<Path, byte[]> contents = new TreeMap<>();
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                contents.put(directory.relativize(file), Files.readAllBytes(file));
            }
        }
        return contents;
    }

    /** A second conform finds every photo within the edge and tiled, so it counts them unchanged and rewrites no byte. */
    @Test
    void conform_isIdempotent() throws IOException {
        placed("large.jpg", image("jpg", 2048, 1536));
        placed("small.png", image("png", 800, 600));
        store.conform("insects");
        Map<Path, byte[]> before = contents(root.resolve("insects"));

        FilesystemImageStore.ConformReport report = store.conform("insects");

        assertThat(report).isEqualTo(new FilesystemImageStore.ConformReport(0, 0, 2));
        Map<Path, byte[]> after = contents(root.resolve("insects"));
        assertThat(after.keySet()).isEqualTo(before.keySet());
        assertThat(after.keySet()).hasSize(4);
        for (Path file : before.keySet()) {
            assertThat(after.get(file)).as("%s", file).isEqualTo(before.get(file));
        }
    }

    /** A file that does not decode is left as it is and counted unchanged, observed at WARN, never thrown; the rest are still conformed. */
    @Test
    void conform_skipsAnUndecodableFile() throws IOException {
        Path junk = placed("junk.jpg", JPEG);
        placed("large.jpg", image("jpg", 2048, 1536));
        var meters = new SimpleMeterRegistry();
        Metrics.addRegistry(meters);
        FilesystemImageStore.ConformReport report;
        try {
            report = store.conform("insects");
        } finally {
            Metrics.removeRegistry(meters);
        }

        assertThat(report).isEqualTo(new FilesystemImageStore.ConformReport(1, 1, 1));
        assertThat(Files.readAllBytes(junk)).isEqualTo(JPEG);
        assertThat(Files.exists(root.resolve("insects/tiles/junk.jpg"))).isFalse();
        assertThat(Files.isRegularFile(root.resolve("insects/tiles/large.jpg"))).isTrue();
        var skipped = meters.find("naturalist.invariant.check")
                .tag("method", "conform")
                .tag("outcome", "violated")
                .tag("level", Level.WARN.tagValue())
                .counter();
        assertThat(skipped).isNotNull();
        assertThat(skipped.count()).isEqualTo(1);
    }
}
