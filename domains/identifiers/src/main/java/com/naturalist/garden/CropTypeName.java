package com.naturalist.garden;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * The name of a crop <em>type</em> — the agronomic category at which requirements are published
 * and planning decisions are made: {@code "tomato"}, {@code "lettuce"}, {@code "basil"}.
 * <p>
 * <b>The inclusion test is whether a lab or an extension service would publish a requirement table
 * for it.</b> Tomato yes; Amish Paste no; <em>Solanum lycopersicum</em> not usually. FGL's March
 * 2026 reports are headed "TOMATO SOIL ANALYSIS" and carry one tomato optimum panel covering every
 * variety in the bed — which is precisely why the type, and not the variety, is what a soil
 * analysis is interpreted against.
 * <p>
 * <b>A type, not an instance.</b> "The 2026 backyard tomato crop" is a season's growing, derivable
 * from the plantings of this type in that zone; it is not this name. Keeping the distinction in
 * the name keeps {@code LabAnalysisInfo.cropType} unambiguous at every call site.
 * <p>
 * <b>Not a taxon.</b> The botanical link is an optional soft {@code PlantSpeciesName} on the garden
 * domain's {@code CropType}; one type spans several species and one species appears as several
 * types.
 * <p>
 * Lived under {@code com.naturalist.soil} until the garden domain existed — an artifact of soil
 * having been built first, not a claim of ownership.
 */
public final class CropTypeName extends EntityName {

    private CropTypeName(String value) {
        super(value);
    }

    @JsonCreator
    public static CropTypeName of(String value) {
        return new CropTypeName(value);
    }

    @Override
    protected int maxLength() {
        return 48;
    }
}
