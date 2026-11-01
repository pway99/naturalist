package com.naturalist.observation;

/** An image read back from an {@link ImageStore}: its bytes and sniffed media type. Read-side transport, no invariants. */
public record StoredImage(byte[] bytes, String mediaType) {
}
