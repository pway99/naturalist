package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed natural key for {@code PlantGenus} entities.
 * <p>
 * The slug is the lowercase kebab form of the Linnaean genus epithet —
 * {@code "thymus"}, {@code "aristolochia"}, {@code "solanum"}. Cross-domain
 * references (e.g. a {@code Plant}'s upward parent reference) carry
 * {@code PlantGenusName} rather than importing {@code plants-api},
 * preserving DAG integrity.
 */
public final class PlantGenusName extends EntityName {

    private PlantGenusName(String value) {
        super(value);
    }

    @JsonCreator
    public static PlantGenusName of(String value) {
        return new PlantGenusName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
