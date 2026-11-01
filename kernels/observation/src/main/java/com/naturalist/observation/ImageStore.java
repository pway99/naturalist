package com.naturalist.observation;

import com.naturalist.data.FileName;

import java.util.Optional;

/**
 * Port for persisting organism image bytes. {@code space} is the domain namespace
 * ("insects", "plants"). An invalid {@link ImageUpload} is the caller's defect: observe it
 * before calling {@link #store}. {@code store} never trusts a client-supplied name; it
 * mints the stored file name itself.
 */
public interface ImageStore {
    FileName store(String space, ImageUpload upload);

    Optional<StoredImage> read(String space, FileName name);
}
