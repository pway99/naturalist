package com.naturalist.observation.filesystem;

import com.naturalist.data.FileName;
import com.naturalist.ddd.EntityId;
import com.naturalist.observability.Observer;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.StoredImage;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * {@link ImageStore} over a directory root. {@code store} writes
 * {@code <root>/<space>/<uuidv7><ext>}; {@code read} looks there first and then falls back
 * to the classpath resource {@code <space>/images/<name>}, which is how seed photos shipped
 * inside a jar are served. Every filesystem call runs under the {@link #STRATEGY} timeout.
 */
@Resilient(name = FilesystemImageStore.STRATEGY)
public final class FilesystemImageStore implements ImageStore {

    public static final String STRATEGY = "image.store";

    private final Observer observer = Observer.forClass(FilesystemImageStore.class);

    private final Path root;

    private final Resilience resilience;

    public FilesystemImageStore(Path root, Resilience resilience) {
        this.root = root;
        this.resilience = resilience;
    }

    @Override
    public FileName store(String space, ImageUpload upload) {
        observer.arguments("store", i -> i.notBlank(space, "space").valueObject(upload, "upload"))
                .throwWhenInvalid();
        Path directory = root.resolve(space).normalize();
        if (!directory.startsWith(root.normalize()) || directory.equals(root.normalize())) {
            throw new IllegalArgumentException("space escapes the store root: " + space);
        }
        String name = EntityId.newUUID() + upload.extension();
        resilience.timeout(STRATEGY).execute(() -> {
            try {
                Files.createDirectories(directory);
                Files.write(directory.resolve(name), upload.bytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return FileName.of(name);
    }

    @Override
    public Optional<StoredImage> read(String space, FileName name) {
        if (name == null || space == null || space.isBlank() || !isPlainName(name.value())) {
            return Optional.empty();
        }
        Path directory = root.resolve(space).normalize();
        Path file = directory.resolve(name.value()).normalize();
        if (!directory.startsWith(root.normalize()) || !file.startsWith(directory)) {
            return Optional.empty();
        }
        byte[] bytes = resilience.timeout(STRATEGY).execute(() -> readBytes(space, name, file));
        return Optional.ofNullable(bytes).map(b -> new StoredImage(b, servedMediaType(b, name)));
    }

    private byte[] readBytes(String space, FileName name, Path file) {
        try {
            if (Files.isRegularFile(file)) {
                return Files.readAllBytes(file);
            }
            String resource = space + "/images/" + name.value();
            try (InputStream in = FilesystemImageStore.class.getClassLoader().getResourceAsStream(resource)) {
                return in == null ? null : in.readAllBytes();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isPlainName(String value) {
        return value != null && !value.isBlank()
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
        if (name == null || space == null || space.isBlank() || !isPlainName(name.value())) {
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
}
