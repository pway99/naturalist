package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;
import com.naturalist.taxonomy.LinealRank;

/**
 * Strongly typed natural key for {@code PlantOrder} entities — the order-rank permit of
 * {@link PlantRankName}, and the top of the plant-side Linnaean chain.
 * <p>
 * The slug is the lowercase kebab form of the Linnaean order epithet —
 * {@code "lamiales"}, {@code "rosales"}, {@code "brassicales"}. Cross-domain references
 * carry {@code PlantOrderName} rather than importing {@code plants-api}, preserving DAG
 * integrity.
 */
public final class PlantOrderName extends EntityName implements PlantRankName {

    private PlantOrderName(String value) {
        super(value);
    }

    @JsonCreator
    public static PlantOrderName of(String value) {
        return new PlantOrderName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }

    @Override
    public LinealRank rank() {
        return LinealRank.ORDER;
    }
}
