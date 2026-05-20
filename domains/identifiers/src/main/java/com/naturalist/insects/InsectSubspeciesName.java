package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed natural key for an insect subspecies-rank catalog entry.
 * <p>
 * The slug is the lowercase kebab form of the Linnaean trinomial —
 * {@code "battus-philenor-hirsuta"}, {@code "limenitis-arthemis-arizonensis"}
 * — three epithets joined by single hyphens. The trinomial is mechanically
 * derivable from a subspecies's parent species slug plus the subspecies
 * epithet, parallel to the binomial pattern on {@link InsectSpeciesName}.
 * <p>
 * No {@code InsectSubspecies} aggregate exists yet; this name type ships
 * ahead of the entity so the {@link InsectRankName} permit list is closed
 * to the full Linnaean range that an {@code InsectImage} may attach to.
 * When a subspecies-rank record first enters the catalog, the entity
 * record's identity slot will be typed {@code InsectSubspeciesName}.
 */
public final class InsectSubspeciesName extends EntityName implements InsectRankName {

    private InsectSubspeciesName(String value) {
        super(value);
    }

    @JsonCreator
    public static InsectSubspeciesName of(String value) {
        return new InsectSubspeciesName(value);
    }

    @Override
    protected int maxLength() {
        return 96;
    }
}
