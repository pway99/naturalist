package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed natural key for {@code Plant} entities.
 * <p>
 * The slug is a stable, URL-safe identifier derived from the plant's common name
 * or binomial epithet. Examples: {@code "california-pipevine"},
 * {@code "crimson-clover"}, {@code "sweet-alyssum"}.
 * <p>
 * Cross-domain references (e.g. zone sub-context noting which plants are present)
 * use {@code PlantName} rather than importing plants-api, preserving DAG integrity.
 */
public final class PlantName extends EntityName {

    private PlantName(String value) {
        super(value);
    }

    @JsonCreator
    public static PlantName of(String value) {
        return new PlantName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
