package com.naturalist.authority;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Textual content retrieved from an external authority's page for a given
 * reference. Used as grounded source material for Durrell description
 * generation — the {@link com.naturalist.textgeneration.TextGenerationService}
 * reshapes this content into four levels rather than generating from
 * training data.
 */
public record AuthorityContent(
        AuthorityReference reference,
        String content
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(reference, "reference")
                .notBlank(content, "content");
    }
}
