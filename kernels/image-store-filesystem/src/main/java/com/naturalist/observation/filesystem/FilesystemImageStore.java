package com.naturalist.observation.filesystem;

import com.naturalist.data.FileName;
import com.naturalist.ddd.EntityId;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Handling;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.observation.ImageRefusedException;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.ImageVariant;
import com.naturalist.observation.StoredImage;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * {@link ImageStore} over a directory root. {@code store} strips the upload of its metadata
 * (refusing it when the strip leaves no image of the same kind, or when it cannot be decoded or
 * declares more than {@link PhotoScaler#MAX_PIXELS} pixels) and writes two files: the photo to
 * {@code <root>/<space>/<uuidv7><ext>} and its tile to {@code <root>/<space>/tiles/<same name>}.
 * A stripped upload within {@link PhotoScaler#PHOTO_EDGE} px is the photo byte for byte; a larger
 * one is stored only as a JPEG of at most that edge, so the original is kept nowhere. The tile is
 * always a JPEG of at most {@link PhotoScaler#TILE_EDGE} px. Each file is written to a temporary
 * file in {@code tiles/} and moved into place with an atomic move (the photo's temporary too,
 * deliberately: conform lists only the space directory, so a temporary is never taken for a photo),
 * the tile before the photo, so a reader never sees a part-written file, a listed photo always has
 * its tile, and a failed store removes its temporary files. {@code read} finds only what the root
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

    private static final Observer observer = Observer.forClass(FilesystemImageStore.class);

    private final Path root;

    private final Resilience resilience;

    public FilesystemImageStore(Path root, Resilience resilience) {
        this.root = root;
        this.resilience = resilience;
    }

    @Override
    public void requireStorable(ImageUpload upload) {
        MethodObserver methodObserver = observer.forMethod("requireStorable");
        methodObserver.arguments(i -> i.valueObject(upload, "upload")).throwWhenInvalid();
        strippedStorable(methodObserver, upload);
    }

    @Override
    @Resilient(name = WRITE_STRATEGY)
    public FileName store(String space, ImageUpload upload) {
        MethodObserver methodObserver = observer.forMethod("store");
        methodObserver.arguments(i -> i
                .notBlank(space, "space")
                .valueObject(upload, "upload")).throwWhenInvalid();
        methodObserver.state(i -> i
                .isTrue(spaceWithinRoot(space), "spaceWithinRoot"))
                .throwWhenInvalid();
        Storable storable = strippedStorable(methodObserver, upload);
        ImageUpload stripped = storable.stripped();
        PhotoScaler.Dimensions declared = storable.declared();
        Path directory = root.resolve(space).normalize();
        // A photo already within the edge is kept as it is, and keeps its format; a larger one becomes a JPEG, and is
        // named as one.
        boolean keepsBytes = withinPhotoEdge(declared);
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
                byte[] photo = null;
                byte[] tile = null;
                try {
                    photo = photo(stripped.bytes(), declared, keepsBytes, orientation);
                    tile = tile(stripped.bytes(), declared, orientation);
                } catch (IllegalArgumentException undecodable) {
                    // the header read but the image did not decode: a refusal of the user's photo, which fitJpeg does
                    // not count, counted once just below (decodable, WARN); an encode fault (ReencodeFailedException)
                    // is a fault, counted by fitJpeg (encoded), and propagates
                }
                boolean decodable = tile != null;
                InvariantObservation decodes = methodObserver.state(i -> i
                        .isTrue(decodable, "decodable"))
                        .observe(Level.WARN);
                if (!decodes.violations().isEmpty()) {
                    // the port's one refusal a caller may answer as a bad photo (ImageRefusedException), counted here
                    throw new ImageRefusedException(decodes.scope(), decodes.violations());
                }
                Path tiles = tileFile.getParent();
                try {
                    if (!abandoned.get()) {
                        Files.createDirectories(tiles);
                    }
                } catch (IOException e) {
                    methodObserver.noticeError("tilesDirectory", e, Handling.WRAPPED);
                    throw new UncheckedIOException(e);
                }
                // The tile lands first, so the photo is never there without it. Each is written to a temporary file in
                // tiles/ and moved into place in one step, so no reader and no racing conform ever sees part of either.
                try {
                    if (!abandoned.get()) {
                        replace(methodObserver, tiles, tileFile, tile);
                    }
                    if (!abandoned.get()) {
                        replace(methodObserver, tiles, photoFile, photo);
                    }
                } finally {
                    if (abandoned.get()) {
                        discard(methodObserver, photoFile, tileFile);
                    }
                }
                return null;
            }));
        } catch (UncheckedIOException | ReencodeFailedException | InvariantViolationException noticed) {
            // counted where it was thrown: a failed tiles directory or write (tilesDirectory, replaced), an image that
            // would not decode (decodable), a decoded image fitJpeg could not re-encode (encoded), or fitJpeg's own
            // argument check; any other IllegalStateException (a cancelled bulkhead permit wait) is counted below
            abandoned.set(true);
            discard(methodObserver, photoFile, tileFile);
            throw noticed;
        } catch (RuntimeException e) {
            abandoned.set(true);
            discard(methodObserver, photoFile, tileFile);
            methodObserver.noticeError("stored", e, Handling.RETHROWN);
            throw e;
        }
        return FileName.of(name);
    }

    @Override
    public Optional<StoredImage> read(String space, FileName name, ImageVariant variant) {
        MethodObserver methodObserver = observer.forMethod("read");
        methodObserver.arguments(i -> i
                .notBlank(space, "space")
                .namedValue(name, "name")
                .notNull(variant, "variant"))
                .throwWhenInvalid();
        methodObserver.state(i -> i
                .isTrue(resolvable(space, name.value()), "resolvable"))
                .throwWhenInvalid();
        Path spaceDirectory = root.resolve(space).normalize();
        Path directory = variant == ImageVariant.TILE ? spaceDirectory.resolve(TILES) : spaceDirectory;
        Path file = directory.resolve(name.value()).normalize();
        methodObserver.state(i -> i
                .isTrue(spaceDirectory.startsWith(root.normalize()), "spaceWithinRoot"))
                .throwWhenInvalid();
        // A read only finds the file and sniffs its first bytes; the caller streams the rest, so no read
        // holds a photo in the heap (PL-16). The bulkhead caps concurrent lookups; past it the caller
        // gets BulkheadFullException.
        Optional<StoredImage> found;
        try {
            found = Optional.ofNullable(resilience.bulkhead(STRATEGY).execute(
                    () -> resilience.timeout(STRATEGY).execute(() -> find(methodObserver, file, name))));
        } catch (UncheckedIOException noticed) {
            // an unreadable file was counted by find (found)
            throw noticed;
        } catch (RuntimeException e) {
            methodObserver.noticeError("read", e, Handling.RETHROWN);
            throw e;
        }
        if (variant == ImageVariant.TILE && found.isEmpty()) {
            // A tile is written with its photo and conform backfills the rest: a missing one is a defect to
            // see, and the photo is never served in its place.
            methodObserver.state(i -> i
                    .isTrue(false, "tilePresent"))
                    .observe(Level.WARN);
        }
        return found;
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
        MethodObserver methodObserver = observer.forMethod("delete");
        methodObserver.arguments(i -> i
                .notBlank(space, "space")
                .namedValue(name, "name"))
                .throwWhenInvalid();
        methodObserver.state(i -> i
                .isTrue(resolvable(space, name.value()), "resolvable"))
                .throwWhenInvalid();
        Path directory = root.resolve(space).normalize();
        Path file = directory.resolve(name.value()).normalize();
        methodObserver.state(i -> i
                .isTrue(directory.startsWith(root.normalize()), "spaceWithinRoot"))
                .throwWhenInvalid();
        Path tile = directory.resolve(TILES).resolve(name.value()).normalize();
        try {
            resilience.timeout(STRATEGY).execute(() -> {
                try {
                    Files.deleteIfExists(file);
                    Files.deleteIfExists(tile);
                } catch (IOException e) {
                    methodObserver.noticeError("removed", e, Handling.WRAPPED);
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException noticed) {
            // a failed removal was counted where it was thrown (removed)
            throw noticed;
        } catch (RuntimeException e) {
            methodObserver.noticeError("deleted", e, Handling.RETHROWN);
            throw e;
        }
    }

    /** An upload stripped of its metadata, with the dimensions its header declares. */
    private record Storable(ImageUpload stripped, PhotoScaler.Dimensions declared) {
    }

    /**
     * The upload stripped of its metadata and the dimensions its header declares, after one observation of what a
     * store keeps: the strip leaves an image of the same kind, and that image can be decoded within the pixel ceiling.
     * Observes through the caller's {@code methodObserver}, and throws with every failing invariant at once.
     */
    private static Storable strippedStorable(MethodObserver methodObserver, ImageUpload upload) {
        ImageUpload stripped = upload.withoutMetadata();
        Optional<PhotoScaler.Dimensions> header = PhotoScaler.declared(stripped.bytes());
        methodObserver.state(i -> i
                .isTrue(stripped.isSameFormatAs(upload), "strippedIsTheSameImage")
                .isTrue(header.isPresent(), "headerDecodes")
                .atMostOrNull(header.map(PhotoScaler.Dimensions::pixels).orElse(null), PhotoScaler.MAX_PIXELS, "pixels"))
                .throwWhenInvalid();
        return new Storable(stripped, header.orElseThrow());
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
     * that cannot be removed is observed through the caller's {@code methodObserver}, never thrown.
     */
    private static void discard(MethodObserver methodObserver, Path... files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException | RuntimeException failed) {
                // the original failure is rethrown by the caller; this one is observed, never thrown
                methodObserver.noticeError("discarded", failed, Handling.SWALLOWED);
            }
        }
    }

    /**
     * The stored file, its size and the media type its leading bytes sniff as, or {@code null} when
     * the root holds no such file. Nothing else is a source. Reads {@link #SNIFF_BYTES} and no more.
     * A file that cannot be read is observed through the caller's {@code methodObserver} before it is thrown.
     */
    private static StoredImage find(MethodObserver methodObserver, Path file, FileName name) {
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
            methodObserver.noticeError("found", e, Handling.WRAPPED);
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

    /**
     * What one {@link #conform} did to a space: how many photos it downscaled in place, how many tiles it wrote, and
     * how many photos it left as they were (already conformed, skipped as undecodable, or gone before conform reached
     * them). A photo both downscaled and tiled counts in both. {@code undecodable} names, by file name within the space,
     * each photo left as it was because it does not decode, so a person can find and replace or remove it.
     */
    public record ConformReport(int downscaled, int tiled, int unchanged, List<String> undecodable) {
    }

    /**
     * Backfills a space to what {@code store} would have written: every regular file directly in {@code <root>/<space>/}
     * over {@link PhotoScaler#PHOTO_EDGE} px is rewritten in place, under its own name, as the JPEG a store keeps, and
     * every photo without a tile gets one. {@code tiles/} itself is never conformed. It runs on the store's own sizing
     * methods and is idempotent: a second run finds nothing to do and counts every photo unchanged. A file that does
     * not decode is left as it is, counted unchanged, observed at WARN and named in the report's {@code undecodable}. A
     * file removed after conform listed the space (a delete racing the run) is counted unchanged and observed as a
     * swallowed failure, and the run carries on. A photo that decodes but cannot be re-encoded is a fault: it is
     * counted at ERROR and thrown with the photo's path in its message, and stops the run as a failed read or write
     * does. A space with no directory yet has nothing to conform. A space outside the root is refused like
     * {@code store} refuses it.
     */
    public ConformReport conform(String space) {
        MethodObserver methodObserver = observer.forMethod("conform");
        methodObserver.arguments(i -> i
                .notBlank(space, "space"))
                .throwWhenInvalid();
        methodObserver.state(i -> i
                .isTrue(spaceWithinRoot(space), "spaceWithinRoot"))
                .throwWhenInvalid();
        Path directory = root.resolve(space).normalize();
        if (!Files.isDirectory(directory)) {
            return new ConformReport(0, 0, 0, List.of());
        }
        // Only the photos directly in the space: tiles/ is a directory, so it is never listed as a photo.
        List<Path> photos;
        try (Stream<Path> entries = Files.list(directory)) {
            photos = entries.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            methodObserver.noticeError("listed", e, Handling.WRAPPED);
            throw new UncheckedIOException(e);
        }
        int downscaled = 0;
        int tiled = 0;
        int unchanged = 0;
        List<String> undecodable = new ArrayList<>();
        for (Path photoFile : photos) {
            Conformed conformed = conformed(methodObserver, photoFile, directory.resolve(TILES).resolve(photoFile.getFileName()));
            downscaled += conformed.downscaled() ? 1 : 0;
            tiled += conformed.tiled() ? 1 : 0;
            unchanged += conformed.downscaled() || conformed.tiled() ? 0 : 1;
            if (conformed.undecodable()) {
                undecodable.add(photoFile.getFileName().toString());
            }
        }
        return new ConformReport(downscaled, tiled, unchanged, List.copyOf(undecodable));
    }

    /** True when the declared longest edge is within {@link PhotoScaler#PHOTO_EDGE}, so the store keeps the bytes as they are. */
    private static boolean withinPhotoEdge(PhotoScaler.Dimensions declared) {
        return declared.longestEdge() <= PhotoScaler.PHOTO_EDGE;
    }

    /** The photo kept for {@code bytes}: the bytes themselves when {@code keepsBytes}, else an upright JPEG fitted to {@link PhotoScaler#PHOTO_EDGE}. */
    private static byte[] photo(byte[] bytes, PhotoScaler.Dimensions declared, boolean keepsBytes, int orientation) {
        return keepsBytes ? bytes : PhotoScaler.fitJpeg(bytes, declared, PhotoScaler.PHOTO_EDGE, orientation);
    }

    /** The tile kept for {@code bytes}: always an upright JPEG fitted to {@link PhotoScaler#TILE_EDGE}. */
    private static byte[] tile(byte[] bytes, PhotoScaler.Dimensions declared, int orientation) {
        return PhotoScaler.fitJpeg(bytes, declared, PhotoScaler.TILE_EDGE, orientation);
    }

    /** What conforming one photo did: rewrote it within the edge, wrote its missing tile, both or neither; and whether it was left as it was because it does not decode. */
    private record Conformed(boolean downscaled, boolean tiled, boolean undecodable) {
    }

    /**
     * Writes {@code bytes} to {@code target} through a temporary file in {@code staging}, moved over the target in one
     * step, so a failure part way leaves the old file or the new one and never a part of either. A failure is observed
     * through the caller's {@code methodObserver} before it is thrown.
     */
    private static void replace(MethodObserver methodObserver, Path staging, Path target, byte[] bytes) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(staging, ".partial-", ".tmp");
            Files.write(temporary, bytes);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            methodObserver.noticeError("replaced", e, Handling.WRAPPED);
            throw new UncheckedIOException(e);
        } finally {
            if (temporary != null) {
                discard(methodObserver, temporary);
            }
        }
    }

    /**
     * Conforms one photo on the store's own sizing path: a photo over {@link PhotoScaler#PHOTO_EDGE} is replaced by its
     * fitted JPEG under the same name, and a missing tile is written. A file that does not decode within the pixel
     * ceiling is left as it is and observed at WARN through the caller's {@code methodObserver}, never thrown. The decode, re-encodes
     * and writes run under the {@link #WRITE_STRATEGY} bulkhead and timeout; each write replaces its file in one step,
     * so a worker the timeout abandons can only leave a conformed file behind, never a part of one.
     */
    private Conformed conformed(MethodObserver methodObserver, Path photoFile, Path tileFile) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(photoFile);
        } catch (NoSuchFileException vanished) {
            // removed since conform listed it (a delete racing the run): nothing is left to conform, and the rest carry on
            methodObserver.noticeError("read", vanished, Handling.SWALLOWED);
            return new Conformed(false, false, false);
        } catch (IOException e) {
            methodObserver.noticeError("read", e, Handling.WRAPPED);
            throw new UncheckedIOException(e);
        }
        Optional<PhotoScaler.Dimensions> header = PhotoScaler.declared(bytes);
        InvariantObservation decodable = methodObserver.state(i -> i
                .isTrue(header.isPresent(), "headerDecodes")
                .atMostOrNull(header.map(PhotoScaler.Dimensions::pixels).orElse(null), PhotoScaler.MAX_PIXELS, "pixels"))
                .observe(Level.WARN);
        if (!decodable.violations().isEmpty()) {
            return new Conformed(false, false, true);
        }
        PhotoScaler.Dimensions declared = header.orElseThrow();
        boolean keepsBytes = withinPhotoEdge(declared);
        boolean tileMissing = !Files.isRegularFile(tileFile);
        if (keepsBytes && !tileMissing) {
            return new Conformed(false, false, false);
        }
        int orientation = ImageUpload.of(bytes).orientation().orElse(1);
        try {
            return resilience.bulkhead(WRITE_STRATEGY).execute(() -> resilience.timeout(WRITE_STRATEGY).execute(() -> {
                byte[] photo;
                byte[] tile;
                try {
                    photo = photo(bytes, declared, keepsBytes, orientation);
                    tile = tileMissing ? tile(bytes, declared, orientation) : null;
                } catch (IllegalArgumentException undecodable) {
                    // the header decoded but the image did not: left as it is, like a file whose header does not decode;
                    // a decoded image that cannot be re-encoded is a fault (ReencodeFailedException) and stops the run
                    methodObserver.state(i -> i
                            .isTrue(false, "decodable"))
                            .observe(Level.WARN);
                    return new Conformed(false, false, true);
                } catch (ReencodeFailedException fault) {
                    // already counted by fitJpeg (encoded); wrapped, not counted again, only so the failure that stops the
                    // run names the photo an operator has to find
                    throw new ReencodeFailedException("photo could not be re-encoded: " + photoFile, fault);
                }
                Path tiles = tileFile.getParent();
                try {
                    Files.createDirectories(tiles);
                } catch (IOException e) {
                    methodObserver.noticeError("tilesDirectory", e, Handling.WRAPPED);
                    throw new UncheckedIOException(e);
                }
                if (tile != null) {
                    replace(methodObserver, tiles, tileFile, tile);
                }
                if (!keepsBytes) {
                    replace(methodObserver, tiles, photoFile, photo);
                }
                return new Conformed(!keepsBytes, tile != null, false);
            }));
        } catch (UncheckedIOException | ReencodeFailedException | InvariantViolationException noticed) {
            // counted where it was thrown: a failed tiles directory or write (tilesDirectory, replaced), a decoded image
            // fitJpeg could not re-encode (encoded, named with its file above), or fitJpeg's own argument check; any other
            // IllegalStateException (a cancelled bulkhead permit wait) is counted below
            throw noticed;
        } catch (RuntimeException e) {
            methodObserver.noticeError("conformed", e, Handling.RETHROWN);
            throw e;
        }
    }
}
