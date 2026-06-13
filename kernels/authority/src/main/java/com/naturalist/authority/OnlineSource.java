package com.naturalist.authority;

import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

public record OnlineSource(
        CitationName name,
        AuthorityReference authorityReference,
        String title,
        @Nullable String author,
        @Nullable Integer year,
        @Nullable Instant lastModified
) implements Citation {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .valueObject(authorityReference, "authorityReference")
                .notBlank(title, "title");
    }
}
