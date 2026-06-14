package com.naturalist.library;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A teaching/reference entry — a human-titled, four-level Durrell
 * {@link Description} of a concept the catalog needs to explain (e.g. what a
 * clade is, how clades relate to taxonomy). A parallel sub-context to
 * {@code Citation} in the library domain; the two never interact.
 */
public record Concept(
        ConceptName name,
        String title,
        Description description
) implements NamedEntity<ConceptName> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notBlank(title, "title")
                .valueObject(description, "description");
    }
}
