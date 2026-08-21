package com.naturalist.plants;

import com.naturalist.ddd.ReadModel;
import com.naturalist.plants.PlantEntityCollections.ImageCollection;

/**
 * The catalog-view read model for a plant at whatever rank identification reached —
 * its rank record composed with the photographic field record. Sealed across the four
 * botanical ranks that carry catalog entities. Identity is the root rank's typed
 * {@link PlantRankName}, returned polymorphically by {@link #name()}. Permits carry no
 * {@code features()} slot; features live on {@link Plant} as a {@code PlantFeatureView}
 * (later chunk). Mirrors {@code InsectTaxonView}.
 */
public sealed interface PlantTaxonView extends ReadModel
        permits PlantOrderView, PlantFamilyView, PlantGenusView, PlantSpeciesView {

    /** The typed slug of the root rank record — polymorphic across the sealed permits. */
    PlantRankName name();

    /** Photographs of this organism at the root rank — non-null, possibly empty. */
    ImageCollection images();
}
