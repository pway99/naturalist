package com.naturalist.library;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Natural key for {@code Concept} — a human-readable slug (e.g. {@code "clade"},
 * {@code "clade-taxonomy-relation"}) stable across deployments. Values match the
 * {@code "name"} field in {@code concepts.json}.
 */
public final class ConceptName extends EntityName {

    private ConceptName(String value) {
        super(value);
    }

    @JsonCreator
    public static ConceptName of(String value) {
        return new ConceptName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
