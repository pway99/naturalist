package com.naturalist.library;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A single entry in the naturalist's glossary — a concise definition of a
 * vernacular term ({@code conspicuous}, {@code connexivum}, {@code voltinism})
 * used across the field guide. A parallel sub-context to {@code Concept} in the
 * library domain; where a {@code Concept} is a page-sized, four-level teaching
 * entry for a big idea, a {@code GlossaryTerm} is a one-line lookup for a word.
 * The two never interact.
 *
 * <p>{@code term} is the display form ("Field mark"); {@code name} is its slug
 * ("field-mark"). {@code example} is an optional usage sentence.
 */
public record GlossaryTerm(
        GlossaryTermName name,
        String term,
        String definition,
        @Nullable String example
) implements NamedEntity<GlossaryTermName> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notBlank(term, "term")
                .notBlank(definition, "definition");
    }
}
