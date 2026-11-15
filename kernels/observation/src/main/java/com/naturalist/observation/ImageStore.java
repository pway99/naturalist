package com.naturalist.observation;

import com.naturalist.data.FileName;

import java.util.Optional;

/**
 * Port for persisting organism image bytes. {@code space} is the domain namespace
 * ("insects", "plants"). An invalid {@link ImageUpload} is the caller's defect: observe it
 * before calling {@link #store}. {@code store} never trusts a client-supplied name; it
 * mints the stored file name itself. Every image a store writes is stripped first
 * ({@link ImageUpload#withoutMetadata()}: a JPEG keeps only its {@code APP0}, {@code APP2} and
 * {@code APP14} application segments and its codec segments, a PNG or WebP loses its Exif, XMP
 * and text chunks, and every format loses the bytes after its end), and an upload that strips to something other than
 * the same kind of image is refused like any other invalid one, with nothing written. Images a
 * store serves without having written them, such as seed photos shipped on the classpath, are
 * stripped once where they are kept, not on every {@link #read}.
 *
 * <p>Size guarantees: a stored photo's longest edge is at most 1024 px, and its tile's at most
 * 640 px; an image is never enlarged. A stripped upload already within 1024 px is stored byte for
 * byte; a larger one is stored only as a re-encoded JPEG of at most 1024 px, and its original is
 * kept nowhere. A store refuses an upload it cannot decode, or whose header declares more than
 * 100,000,000 pixels, without decoding it ({@link #requireStorable}).
 */
public interface ImageStore {
    FileName store(String space, ImageUpload upload);

    /** The photo, or the tile, of a stored image; empty when it is not held. */
    Optional<StoredImage> read(String space, FileName name, ImageVariant variant);

    /** The full-size photo: {@code read(space, name, ImageVariant.PHOTO)}. */
    default Optional<StoredImage> read(String space, FileName name) {
        return read(space, name, ImageVariant.PHOTO);
    }

    /**
     * Refuses, through the Observer ({@code storable}) with an {@code InvariantViolationException},
     * an upload {@link #store} could not keep: undecodable, or declaring more than 100,000,000
     * pixels. Reads the header only and writes nothing, so a caller can check before it spends
     * anything on the upload.
     */
    void requireStorable(ImageUpload upload);

    /**
     * Removes a stored image, best-effort: a missing file is not an error, and a name that
     * would escape the space (path separators or {@code ..}) is silently ignored. Intended for
     * discarding an orphaned upload when persistence fails after {@link #store}.
     */
    void delete(String space, FileName name);
}
