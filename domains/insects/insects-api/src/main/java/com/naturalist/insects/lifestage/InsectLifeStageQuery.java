package com.naturalist.insects.lifestage;

import com.naturalist.data.EntityQuery;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageName;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;

/**
 * Namespace query for the insect life stage sub-context — the single discoverable
 * entry point for reading {@link LifeStage} data.
 *
 * <p>Mirrors {@code InsectQuery}: the outer namespace is a public interface, the
 * nested entity query carries the {@link EntityQuery} contract.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * insectLifeStageQuery.lifeStages().getByName(lifeStageName);
 * insectLifeStageQuery.lifeStages().forSpeciesName(speciesName);
 * }</pre>
 */
public interface InsectLifeStageQuery {

    LifeStageEntityQuery lifeStages();

    interface LifeStageEntityQuery
            extends EntityQuery<LifeStageName, LifeStage, LifeStageCollection> {

        LifeStageCollection forSpeciesName(InsectSpeciesName speciesName);
    }
}