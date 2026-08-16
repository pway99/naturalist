package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;
import com.naturalist.taxonomy.LinealRank;

/**
 * Strongly typed natural key for {@link PlantSpecies} entities — the <b>species-rank</b>
 * permit of {@link PlantRankName}.
 * <p>
 * The slug is a stable, URL-safe identifier, normally the lowercased binomial.
 * Examples: {@code "aristolochia-californica"}, {@code "trifolium-incarnatum"},
 * {@code "lobularia-maritima"}.
 * <p>
 * Named {@code PlantName} until 2026-08-15. The bare name asserted a primacy that does not
 * exist — a plant is classified on three independent axes (Linnaean rank, cultivar, and
 * agronomic crop type), so no single type is <em>the</em> name of a plant. Insects never
 * had the problem: it has no bare {@code InsectName}, every insect name stating its rank.
 * <p>
 * Cross-domain references (e.g. garden recording what was planted) use
 * {@code PlantSpeciesName} rather than importing plants-api, preserving DAG integrity.
 */
public final class PlantSpeciesName extends EntityName implements PlantRankName {

    private PlantSpeciesName(String value) {
        super(value);
    }

    @JsonCreator
    public static PlantSpeciesName of(String value) {
        return new PlantSpeciesName(value);
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
