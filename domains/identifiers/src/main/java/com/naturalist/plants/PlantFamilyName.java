package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed natural key for {@code PlantFamily} entities.
 * <p>
 * The slug is the lowercase kebab form of the Linnaean family epithet —
 * {@code "lamiaceae"}, {@code "solanaceae"}, {@code "aristolochiaceae"}.
 * Cross-domain references (e.g. a {@code PlantGenus}'s upward parent
 * reference) carry {@code PlantFamilyName} rather than importing
 * {@code plants-api}, preserving DAG integrity.
 */
public final class PlantFamilyName extends EntityName {

    private PlantFamilyName(String value) {
        super(value);
    }

    @JsonCreator
    public static PlantFamilyName of(String value) {
        return new PlantFamilyName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
