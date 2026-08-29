package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.ddd.EntityId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores uploaded images to the filesystem under UUID filenames. Validates
 * content type via magic bytes — rejects anything that isn't JPEG, PNG, or WebP.
 * <p>
 * Storage directory is configurable; defaults to {@code data/images/insects/}
 * relative to the working directory.
 */
class ImageStorageService {

    private static final int MAX_SIZE = 20 * 1024 * 1024; // 20 MB

    // JPEG: FF D8 FF
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    // PNG: 89 50 4E 47
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    // WebP: RIFF....WEBP (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    private final Path storageDir;

    ImageStorageService(Path storageDir) {
        this.storageDir = storageDir;
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create image storage directory: " + storageDir, e);
        }
    }

    FileName store(byte[] imageBytes) {
        validateSize(imageBytes);
        var extension = detectExtension(imageBytes);
        var uuidName = EntityId.newUUID().toString() + extension;
        var target = storageDir.resolve(uuidName);
        try {
            // Ensure the directory each write: the constructor creates it eagerly, but a
            // long-lived service must not fail a store if the directory is removed later
            // (ops disk cleanup in production; a sibling test's teardown in the suite).
            Files.createDirectories(storageDir);
            Files.write(target, imageBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store image: " + target, e);
        }
        return FileName.of(uuidName);
    }

    Path resolve(String filename) {
        // Prevent path traversal
        var resolved = storageDir.resolve(filename).normalize();
        if (!resolved.startsWith(storageDir)) {
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }
        return resolved;
    }

    private void validateSize(byte[] imageBytes) {
        if (imageBytes.length > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Image exceeds maximum size of 20 MB (got " + imageBytes.length + " bytes)");
        }
        if (imageBytes.length < 4) {
            throw new IllegalArgumentException("Image data too small to be valid");
        }
    }

    private String detectExtension(byte[] bytes) {
        if (startsWith(bytes, JPEG_MAGIC)) return ".jpg";
        if (startsWith(bytes, PNG_MAGIC)) return ".png";
        if (bytes.length >= 12 && startsWith(bytes, RIFF_MAGIC)
                && bytes[8] == WEBP_MAGIC[0] && bytes[9] == WEBP_MAGIC[1]
                && bytes[10] == WEBP_MAGIC[2] && bytes[11] == WEBP_MAGIC[3]) {
            return ".webp";
        }
        throw new IllegalArgumentException(
                "Unsupported image format. Only JPEG, PNG, and WebP are accepted.");
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
