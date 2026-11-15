package com.naturalist.observation.filesystem;

import com.naturalist.data.FileName;
import com.naturalist.ddd.EntityId;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.ImageVariant;
import com.naturalist.observation.StoredImage;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link ImageStore} over a directory root. {@code store} strips the upload of its metadata
 * (refusing it when the strip leaves no image of the same kind, or when it cannot be decoded or
 * declares more than {@link PhotoScaler#MAX_PIXELS} pixels) and writes two files: the photo to
 * {@code <root>/<space>/<uuidv7><ext>} and its tile to {@code <root>/<space>/tiles/<same name>}.
 * A stripped upload within {@link PhotoScaler#PHOTO_EDGE} px is the photo byte for byte; a larger
 * one is stored only as a JPEG of at most that edge, so the original is kept nowhere. The tile is
 * always a JPEG of at most {@link PhotoScaler#TILE_EDGE} px. {@code read} finds only what the root
 * holds, and hands back a stream of it rather than its bytes; a missing tile is observed and
 * answered empty, never replaced by the photo. Photos are data, never classpath resources: the
 * catalog's seed photos reach the root the same way as any file, copied there by the seeder
 * ({@code db-init} in a deployment). Reads and deletes run under the {@link #STRATEGY} timeout
 * (reads also its bulkhead); a store runs its decode, re-encode and both writes under the
 * {@link #WRITE_STRATEGY} bulkhead and timeout. When that fails or times out the caller removes
 * both files and flags the store abandoned, and a worker the timeout could not interrupt checks the
 * flag before each write and removes its own files when it finishes, so none is left behind. Known
 * limit: the bulkhead permit is released when the caller's timeout fires while an abandoned worker
 * may still hold its raster, so the heap bound is only as tight as the timeout.
 */
@Resilient(name = FilesystemImageStore.STRATEGY)
public final class FilesystemImageStore implements ImageStore {

    public static final String STRATEGY = "image.store";

    /** Bounds a store: the decode, the re-encodes and both writes, which are CPU and heap heavy. */
    public static final String WRITE_STRATEGY = "image.store.write";

    private static final String TILES = "tiles";

    /** Enough leading bytes for every magic {@link ImageUpload#mediaType()} recognises. */
    private static final int SNIFF_BYTES = 12;

    private final Observer observer = Observer.forClass(FilesystemImageStore.class);

    private final Path root;

    private final Resilience resilience;

    public FilesystemImageStore(Path root, Resilience resilience) {
        this.root = root;
        this.resilience = resilience;
    }

    @Override
    public void requireStorable(ImageUpload upload) {
        MethodObserver m = observer.forMethod("requireStorable");
        m.arguments(i -> i.valueObject(upload, "upload")).throwWhenInvalid();
        strippedStorable(m, upload);
    }

    @Override
    @Resilient(name = WRITE_STRATEGY)
    public FileName store(String space, ImageUpload upload) {
        MethodObserver m = observer.forMethod("store");
        m.arguments(i -> i
            .notBlank(space, "space")
            .valueObject(upload, "upload")).throwWhenInvalid();
        m.arguments(i -> i
            .isTrue(spaceWithinRoot(space), "spaceWithinRoot"))
            .throwWhenInvalid();
        ImageUpload stripped = strippedStorable(m, upload);
        Path directory = root.resolve(space).normalize();
        // Storable above means the header decodes, so declared() answers; orElse(false) only keeps this total
        // rather than throwing. A photo already within the edge is kept as it is, and keeps its format; a larger
        // one becomes a JPEG, and is named as one.
        boolean keepsBytes = PhotoScaler.declared(stripped.bytes())
                .map(declared -> declared.longestEdge() <= PhotoScaler.PHOTO_EDGE)
                .orElse(false);
        String name = EntityId.newUUID() + (keepsBytes ? stripped.extension() : ".jpg");
        Path photoFile = directory.resolve(name);
        Path tileFile = directory.resolve(TILES).resolve(name);
        int orientation = stripped.orientation().orElse(1);
        // Set by the caller when the store fails or times out. The timeout adapter never interrupts its worker, so
        // the worker may still be encoding: it checks this before each write and cleans up after itself, and
        // whichever of caller and worker finishes last removes the files.
        AtomicBoolean abandoned = new AtomicBoolean();
        try {
            resilience.bulkhead(WRITE_STRATEGY).execute(() -> resilience.timeout(WRITE_STRATEGY).execute(() -> {
                byte[] photo = keepsBytes
                        ? stripped.bytes()
                        : PhotoScaler.fitJpeg(stripped.bytes(), PhotoScaler.PHOTO_EDGE, orientation);
                byte[] tile = PhotoScaler.fitJpeg(stripped.bytes(), PhotoScaler.TILE_EDGE, orientation);
                try {
                    if (!abandoned.get()) {
                        Files.createDirectories(tileFile.getParent());
                        Files.write(photoFile, photo);
                    }
                    if (!abandoned.get()) {
                        Files.write(tileFile, tile);
                    }
                } catch (IOException e) {
                    m.state(i -> i
                        .isTrue(false, "written"))
                        .observe(Level.ERROR);
                    throw new UncheckedIOException(e);
                } finally {
                    if (abandoned.get()) {
                        discard(m, photoFile, tileFile);
                    }
                }
                return null;
            }));
        } catch (RuntimeException e) {
            abandoned.set(true);
            discard(m, photoFile, tileFile);
            m.state(i -> i
                .isTrue(false, "stored"))
                .observe(Level.ERROR);
            throw e;
        }
        return FileName.of(name);
    }

    @Override
    public Optional<StoredImage> read(String space, FileName name, ImageVariant variant) {
        MethodObserver m = observer.forMethod("read");
        if (name == null || variant == null || StringUtils.isBlank(space) || !isPlainName(name.value())
                || !resolvable(space, name.value())) {
            return Optional.empty();
        }
        Path spaceDirectory = root.resolve(space).normalize();
        Path directory = variant == ImageVariant.TILE ? spaceDirectory.resolve(TILES) : spaceDirectory;
        Path file = directory.resolve(name.value()).normalize();
        if (!spaceDirectory.startsWith(root.normalize()) || !file.startsWith(directory)) {
            return Optional.empty();
        }
        // A read only finds the file and sniffs its first bytes; the caller streams the rest, so no read
        // holds a photo in the heap (PL-16). The bulkhead caps concurrent lookups; past it the caller
        // gets BulkheadFullException.
        Optional<StoredImage> found;
        try {
            found = Optional.ofNullable(resilience.bulkhead(STRATEGY).execute(
                    () -> resilience.timeout(STRATEGY).execute(() -> find(m, file, name))));
        } catch (RuntimeException e) {
            m.state(i -> i
                .isTrue(false, "read"))
                .observe(Level.ERROR);
            throw e;
        }
        if (variant == ImageVariant.TILE && found.isEmpty()) {
            // A tile is written with its photo and conform backfills the rest: a missing one is a defect to
            // see, and the photo is never served in its place.
            m.state(i -> i
                .isTrue(false, "tilePresent"))
                .observe(Level.WARN);
        }
        return found;
    }

    private static boolean isPlainName(String value) {
        return StringUtils.isNotBlank(value)
                && !value.contains("/") && !value.contains("\\") && !value.contains("..");
    }

    private static String mediaType(FileName name) {
        return switch (name.nameType()) {
            case "JPG", "JPEG" -> "image/jpeg";
            case "PNG" -> "image/png";
            case "WEBP" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /**
     * The media type the bytes sniff as (JPEG, PNG or WebP magic); the file name's extension
     * only when the bytes are not a recognised image, and {@code application/octet-stream} last.
     */
    private static String servedMediaType(byte[] bytes, FileName name) {
        String sniffed = ImageUpload.of(bytes).mediaType();
        return sniffed != null ? sniffed : mediaType(name);
    }

    @Override
    public void delete(String space, FileName name) {
        MethodObserver m = observer.forMethod("delete");
        if (name == null || StringUtils.isBlank(space) || !isPlainName(name.value())
                || !resolvable(space, name.value())) {
            return;
        }
        Path directory = root.resolve(space).normalize();
        Path file = directory.resolve(name.value()).normalize();
        if (!directory.startsWith(root.normalize()) || !file.startsWith(directory)) {
            return;
        }
        Path tile = directory.resolve(TILES).resolve(name.value()).normalize();
        try {
            resilience.timeout(STRATEGY).execute(() -> {
                try {
                    Files.deleteIfExists(file);
                    Files.deleteIfExists(tile);
                } catch (IOException e) {
                    m.state(i -> i
                        .isTrue(false, "removed"))
                        .observe(Level.ERROR);
                    throw new UncheckedIOException(e);
                }
            });
        } catch (RuntimeException e) {
            m.state(i -> i
                .isTrue(false, "deleted"))
                .observe(Level.ERROR);
            throw e;
        }
    }

    /**
     * The upload stripped of its metadata, after one observation of what a store keeps: the strip leaves an image
     * of the same kind, and that image can be decoded within the pixel ceiling. Observes through the caller's
     * {@code m}, and throws with every failing invariant at once.
     */
    private static ImageUpload strippedStorable(MethodObserver m, ImageUpload upload) {
        ImageUpload stripped = upload.withoutMetadata();
        m.state(i -> i
            .isTrue(stripped.isSameFormatAs(upload), "strippedIsTheSameImage")
            .isTrue(PhotoScaler.storable(stripped.bytes()), "storable"))
            .throwWhenInvalid();
        return stripped;
    }

    /** True when {@code space} names a directory strictly inside the root, so a store never writes beside or into it. */
    private boolean spaceWithinRoot(String space) {
        if (!resolvable(space)) {
            return false;
        }
        Path directory = root.resolve(space).normalize();
        return directory.startsWith(root.normalize()) && !directory.equals(root.normalize());
    }

    /**
     * Best-effort removal of a half-written pair; the failure being propagated is the one that matters. A file
     * that cannot be removed is observed through the caller's {@code m}, never thrown.
     */
    private static void discard(MethodObserver m, Path... files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException | RuntimeException failed) {
                // the original failure is rethrown by the caller; this one is observed, never thrown
                m.state(i -> i
                    .isTrue(false, "discarded"))
                    .observe(Level.ERROR);
            }
        }
    }

    /**
     * The stored file, its size and the media type its leading bytes sniff as, or {@code null} when
     * the root holds no such file. Nothing else is a source. Reads {@link #SNIFF_BYTES} and no more.
     * A file that cannot be read is observed through the caller's {@code m} before it is thrown.
     */
    private static StoredImage find(MethodObserver m, Path file, FileName name) {
        try {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            long size = Files.size(file);
            byte[] head;
            try (InputStream in = Files.newInputStream(file)) {
                head = in.readNBytes(SNIFF_BYTES);
            }
            return new StoredImage(servedMediaType(head, name), size, () -> Files.newInputStream(file));
        } catch (IOException e) {
            m.state(i -> i
                .isTrue(false, "found"))
                .observe(Level.ERROR);
            throw new UncheckedIOException(e);
        }
    }

    /** True when every part is a path string the file system accepts, so resolving it against the root cannot throw. */
    private boolean resolvable(String... parts) {
        try {
            for (String part : parts) {
                root.resolve(part);
            }
            return true;
        } catch (InvalidPathException unresolvable) {
            // a string the file system cannot name (a NUL, say) is refused as a bad space or name, never thrown
            return false;
        }
    }
}
