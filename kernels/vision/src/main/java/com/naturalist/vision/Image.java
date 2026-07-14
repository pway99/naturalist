package com.naturalist.vision;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record Image(
        byte[] bytes,
        String mediaType,
        ImageMetadata metadata
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(bytes, "bytes")
                .notBlank(mediaType, "mediaType")
                .notNull(metadata, "metadata");
    }
}
