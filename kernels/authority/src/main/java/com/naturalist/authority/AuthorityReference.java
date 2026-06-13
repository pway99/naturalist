package com.naturalist.authority;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.net.URI;
import java.util.function.Consumer;

/**
 * A deep-link pointer into an external authority's catalogue for some
 * subject. Carries its own {@link AuthoritySource} so it is
 * self-describing once stored or passed around. The {@code url} is a
 * fully resolvable deep-link, precomputed by the producing client.
 */
public record AuthorityReference(AuthoritySource source, URI url) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(source, "source")
                .notNull(url, "url");
    }
}