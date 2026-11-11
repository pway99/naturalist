package com.naturalist.observation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * An image found in an {@link ImageStore}: its media type, its size in bytes, and a way to open
 * its content. The media type is sniffed from the leading bytes (JPEG, PNG or WebP magic),
 * falling back to the file name's extension when they are not a recognised image. Read-side
 * transport, no invariants.
 *
 * <p>The content is not held: a photo may be {@link ImageUpload#MAX_BYTES}, so a server streams it
 * from {@link Content#open()} to the client instead of buffering it, and the heap a read costs does
 * not grow with how many are being served (PL-16). Each {@code open} is a fresh stream the caller
 * closes.
 */
public record StoredImage(String mediaType, long size, Content content) {

    /** Opens the stored bytes. The image may have been deleted since the read that found it. */
    @FunctionalInterface
    public interface Content {
        InputStream open() throws IOException;
    }

    /**
     * Every byte, in the heap. For checks over one image (tests, metadata assertions); never for
     * serving, which streams {@link #content()}.
     */
    public byte[] readAllBytes() {
        try (InputStream in = content.open()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
