package com.naturalist.plants.heritage;

import com.naturalist.data.EntityQuery;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.plants.heritage.SeedLineageEntityCollections.SeedLineageCollection;

/**
 * Namespace query for the heritage sub-context — the single discoverable
 * entry point for reading seed-lineage data.
 */
public interface SeedLineageQuery {

    SeedLineageEntityQuery lineages();

    interface SeedLineageEntityQuery extends EntityQuery<SeedLineageName, SeedLineage, SeedLineageCollection> {

        /**
         * All lineages recorded for a given cultivar — the natural cultivar → lineages rollup.
         */
        SeedLineageCollection forCultivarName(CultivarName cultivarName);
    }
}
