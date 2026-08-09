package com.naturalist.library;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Natural key for {@code GlossaryTerm} — a human-readable slug (e.g.
 * {@code "conspicuous"}, {@code "field-mark"}) stable across deployments.
 * Values match the {@code "name"} field in {@code glossary-terms.json}.
 */
public final class GlossaryTermName extends EntityName {

    private GlossaryTermName(String value) {
        super(value);
    }

    @JsonCreator
    public static GlossaryTermName of(String value) {
        return new GlossaryTermName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
