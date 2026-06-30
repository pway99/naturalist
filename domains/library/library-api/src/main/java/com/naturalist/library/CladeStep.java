package com.naturalist.library;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinealRank;

import java.util.Optional;
import java.util.function.Consumer;

public record CladeStep(
        String cladeSlug,
        String displayName,
        Optional<LinealRank> rank
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(cladeSlug, "cladeSlug")
                .notBlank(displayName, "displayName")
                .notNull(rank, "rank");
    }
}
