package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed natural key for {@code Plant} entities.
 * <p>
 * The slug is a stable, URL-safe identifier, normally the lowercased binomial.
 * Examples: {@code "aristolochia-californica"}, {@code "trifolium-incarnatum"},
 * {@code "lobularia-maritima"}.
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
