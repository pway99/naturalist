package com.naturalist.garden;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * The natural-key name of a cultivated variety (e.g. {@code "amish-paste"},
 * {@code "san-marzano"}). A cultivar belongs to exactly one {@link CropName}.
 * <p>
 * A cultivar is not a taxon. {@code amish-paste} and {@code san-marzano} are both
 * <em>Solanum lycopersicum</em>; what distinguishes them is horticultural selection, not
 * phylogeny. The botanical link, where it matters, is a soft {@code PlantName} on the crop.
 */
public final class CultivarName extends EntityName {

    private CultivarName(String value) {
        super(value);
    }

    @JsonCreator
    public static CultivarName of(String value) {
        return new CultivarName(value);
    }

    @Override
    protected int maxLength() {
        return 48;
    }
}
