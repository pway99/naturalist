package com.naturalist.observation;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Raw bytes of an uploaded image. Invariants: the content sniffs as JPEG, PNG or WebP
 * (by magic bytes, never by client-supplied name or type) and is at most {@link #MAX_BYTES}.
 * Equality is by content.
 */
public record ImageUpload(byte[] bytes) implements ValueObject {
    public static final int MAX_BYTES = 20 * 1024 * 1024;

    private static final Observer observer = Observer.forClass(ImageUpload.class);

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

    /**
     * The point the camera recorded in the upload's Exif GPS tags, read from a JPEG {@code APP1},
     * a PNG {@code eXIf} or a WebP {@code EXIF} block. Empty when the upload carries no fix, and
     * when the metadata is truncated or corrupt or decodes to no point on the globe: a bad fix is
     * observed, never thrown and never handed on.
     */
    public Optional<Coordinates> gps() {
        MethodObserver m = observer.forMethod("gps");
        return ImageMetadataSegments.exifTiff(bytes, m).flatMap(tiff -> ImageMetadataSegments.gps(tiff, m));
    }

    /**
     * This upload with its metadata removed, so what is stored says nothing the picture does not:
     * a JPEG loses its Exif and XMP {@code APP1}, IPTC {@code APP13} and {@code COM} segments, a
     * PNG its {@code eXIf} and text chunks, a WebP its {@code EXIF} and {@code XMP } chunks. The
     * one tag kept is a JPEG's Exif orientation, written back alone, so the picture still stands
     * the right way up. Everything a decoder reads is copied byte for byte; nothing is re-encoded.
     * Stripping a stripped upload changes nothing. When the container is malformed, the blocks
     * read before the fault are still removed, the rest is kept as it stands, and the fault is
     * observed. Returns this upload when there was nothing to remove, and for bytes that are not
     * a recognised image.
     */
    public ImageUpload withoutMetadata() {
        if (mediaType() == null) return this;
        byte[] stripped = ImageMetadataSegments.withoutMetadata(bytes, observer.forMethod("withoutMetadata"));
        return Arrays.equals(stripped, bytes) ? this : new ImageUpload(stripped);
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

    /**
     * True when this upload satisfies its own {@link #invariants()}: it sniffs as JPEG, PNG or
     * WebP and is at most {@link #MAX_BYTES}. The same conditions, as a plain predicate for
     * callers that only need the verdict.
     */
    public boolean isValid() {
        return mediaType() != null && bytes != null && bytes.length <= MAX_BYTES;
    }
}
