package com.naturalist.observation;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Raw bytes of an uploaded image. Invariants: the content sniffs as JPEG, PNG or WebP
 * (by magic bytes, never by client-supplied name or type) and is at most {@link #MAX_BYTES}.
 * Equality is by content.
 */
public record ImageUpload(byte[] bytes) implements ValueObject {
    public static final int MAX_BYTES = 20 * 1024 * 1024;

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};

    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};

    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    public static ImageUpload of(byte[] bytes) {
        return new ImageUpload(bytes);
    }

    public String mediaType() {
        if (bytes == null || bytes.length < 12) return null;
        if (startsWith(JPEG_MAGIC, 0)) return "image/jpeg";
        if (startsWith(PNG_MAGIC, 0)) return "image/png";
        if (startsWith(RIFF_MAGIC, 0) && startsWith(WEBP_MAGIC, 8)) return "image/webp";
        return null;
    }

    public String extension() {
        String type = mediaType();
        if (type == null) return null;
        return switch (type) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> ".webp";
        };
    }

    private boolean startsWith(byte[] prefix, int offset) {
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[offset + i] != prefix[i]) return false;
        }
        return true;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .isTrue(mediaType() != null, "format")
                .isTrue(bytes != null && bytes.length <= MAX_BYTES, "size");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ImageUpload u && Arrays.equals(bytes, u.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return "ImageUpload[" + (bytes == null ? 0 : bytes.length) + " bytes]";
    }
}
