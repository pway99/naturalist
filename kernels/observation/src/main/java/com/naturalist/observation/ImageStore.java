package com.naturalist.observation;

import com.naturalist.data.FileName;

import java.util.Optional;

/**
 * Port for persisting organism image bytes. {@code space} is the domain namespace
 * ("insects", "plants"). An invalid {@link ImageUpload} is the caller's defect: observe it
 * before calling {@link #store}. {@code store} never trusts a client-supplied name; it
 * mints the stored file name itself. Every image a store writes is stripped first
 * ({@link ImageUpload#withoutMetadata()}), and an upload that strips to something other than
 * the same kind of image is refused like any other invalid one, with nothing written. Images a
 * store serves without having written them, such as seed photos shipped on the classpath, are
 * stripped once where they are kept, not on every {@link #read}.
 */
public interface ImageStore {
    FileName store(String space, ImageUpload upload);

    Optional<StoredImage> read(String space, FileName name);

    /**
     * Removes a stored image, best-effort: a missing file is not an error, and a name that
     * would escape the space (path separators or {@code ..}) is silently ignored. Intended for
     * discarding an orphaned upload when persistence fails after {@link #store}.
     */
    void delete(String space, FileName name);
}
