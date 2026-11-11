package com.naturalist.observation.filesystem;

import com.naturalist.data.FileName;
import com.naturalist.ddd.EntityId;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.StoredImage;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * {@link ImageStore} over a directory root. {@code store} writes the upload, stripped of its
 * metadata and refused when the strip leaves no image of the same kind, to
 * {@code <root>/<space>/<uuidv7><ext>}; {@code read} finds only what the root holds, and hands back
 * a stream of it rather than its bytes. Photos are
 * data, never classpath resources: the catalog's seed photos reach the root the same way as any
 * file, copied there by the seeder ({@code db-init} in a deployment). Every filesystem call runs
 * under the {@link #STRATEGY} timeout.
 */
@Resilient(name = FilesystemImageStore.STRATEGY)
public final class FilesystemImageStore implements ImageStore {

    public static final String STRATEGY = "image.store";

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
    public FileName store(String space, ImageUpload upload) {
        MethodObserver m = observer.forMethod("store");
        m.arguments(i -> i.notBlank(space, "space").valueObject(upload, "upload")).throwWhenInvalid();
        ImageUpload stripped = upload.withoutMetadata();
        m.state(i -> i.isTrue(stripped.isSameFormatAs(upload), "strippedIsTheSameImage")).throwWhenInvalid();
        Path directory = root.resolve(space).normalize();
        if (!directory.startsWith(root.normalize()) || directory.equals(root.normalize())) {
            throw new IllegalArgumentException("space escapes the store root: " + space);
        }
        String name = EntityId.newUUID() + stripped.extension();
        resilience.timeout(STRATEGY).execute(() -> {
            try {
                Files.createDirectories(directory);
                Files.write(directory.resolve(name), stripped.bytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return FileName.of(name);
    }

    @Override
    public Optional<StoredImage> read(String space, FileName name) {
        if (name == null || StringUtils.isBlank(space) || !isPlainName(name.value())) {
            return Optional.empty();
        }
        Path directory = root.resolve(space).normalize();
        Path file = directory.resolve(name.value()).normalize();
        if (!directory.startsWith(root.normalize()) || !file.startsWith(directory)) {
            return Optional.empty();
        }
        // A read only finds the file and sniffs its first bytes; the caller streams the rest, so no read
        // holds a photo in the heap (PL-16). The bulkhead caps concurrent lookups; past it the caller
        // gets BulkheadFullException.
        return Optional.ofNullable(resilience.bulkhead(STRATEGY).execute(
                () -> resilience.timeout(STRATEGY).execute(() -> find(file, name))));
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
        if (name == null || StringUtils.isBlank(space) || !isPlainName(name.value())) {
            return;
        }
        Path directory = root.resolve(space).normalize();
        Path file = directory.resolve(name.value()).normalize();
        if (!directory.startsWith(root.normalize()) || !file.startsWith(directory)) {
            return;
        }
        resilience.timeout(STRATEGY).execute(() -> {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * The stored file, its size and the media type its leading bytes sniff as, or {@code null} when
     * the root holds no such file. Nothing else is a source. Reads {@link #SNIFF_BYTES} and no more.
     */
    private static StoredImage find(Path file, FileName name) {
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
            throw new UncheckedIOException(e);
        }
    }
}
