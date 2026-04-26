package com.naturalist.chemistry.compound;

/**
 * Chemical nature axis — organic vs. inorganic vs. organometallic.
 * <p>
 * Organic compounds are characterised by C–H bonds and their derivatives.
 * Organometallic compounds contain at least one carbon-to-metal bond.
 * Carbonates, oxides, cyanides, and pure carbon allotropes are conventionally
 * classified as {@link #INORGANIC} despite containing carbon.
 */
public enum ChemicalNature {
    ORGANIC,
    INORGANIC,
    ORGANOMETALLIC
}