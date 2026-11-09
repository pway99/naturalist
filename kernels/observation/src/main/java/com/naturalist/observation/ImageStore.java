package com.naturalist.observation;

import com.naturalist.data.FileName;

import java.util.Optional;

/**
 * Port for persisting organism image bytes. {@code space} is the domain namespace
 * ("insects", "plants"). An invalid {@link ImageUpload} is the caller's defect: observe it
 * before calling {@link #store}. {@code store} never trusts a client-supplied name; it
 * mints the stored file name itself. An implementation stores the upload with its metadata
 * removed ({@link ImageUpload#withoutMetadata()}); what {@link #read} returns never carries
 * metadata.
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
