package com.naturalist.vision;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

public record ImageMetadata(
        @Nullable String location,
        @Nullable Instant capturedAt
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {};
    }
}
