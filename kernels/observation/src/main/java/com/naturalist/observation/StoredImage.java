package com.naturalist.observation;

/**
 * An image read back from an {@link ImageStore}: its bytes and media type. The media type is
 * sniffed from the bytes (JPEG, PNG or WebP magic), falling back to the file name's extension
 * when the bytes are not a recognised image. Read-side transport, no invariants.
 */
public record StoredImage(byte[] bytes, String mediaType) {
}
