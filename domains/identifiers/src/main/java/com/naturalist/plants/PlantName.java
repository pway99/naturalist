package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;
import com.naturalist.taxonomy.LinealRank;

/**
 * Strongly typed natural key for {@code Plant} entities — the <b>species-rank</b> permit
 * of {@link PlantRankName}.
 * <p>
 * The slug is a stable, URL-safe identifier, normally the lowercased binomial.
 * Examples: {@code "aristolochia-californica"}, {@code "trifolium-incarnatum"},
 * {@code "lobularia-maritima"}.
 * <p>
 * The unqualified name predates the rank layer; {@code PlantFamilyName} and
 * {@code PlantGenusName} say their rank and this one does not. A rename to
 * {@code PlantSpeciesName} is tracked in the plants consistency plan — it is deferred
 * rather than rejected, because it touches every module that references a plant by name.
 * <p>
 * Cross-domain references (e.g. zone sub-context noting which plants are present)
 * use {@code PlantName} rather than importing plants-api, preserving DAG integrity.
 */
public final class PlantName extends EntityName implements PlantRankName {

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

    @Override
    public LinealRank rank() {
        return LinealRank.SPECIES;
    }
}
