package com.naturalist.plants.phytochemistry;

import com.naturalist.data.EntityQuery;
import com.naturalist.plants.PlantRankName;
import com.naturalist.chemistry.compound.CompoundName;

/**
 * Read surface for the phytochemistry sub-context. N=1 collapse (ADR-020): the sub-context
 * holds a single entity, so this query <em>is</em> the entity query — no wrapping
 * namespace, no accessor. Mirrors the top-level convention insects uses.
 */
public interface PhytochemicalConstituentQuery extends EntityQuery<PhytochemicalConstituentName, PhytochemicalConstituent, PhytochemicalConstituentCollection> {

    PhytochemicalConstituentCollection forPlantName(PlantRankName plantRankName);

    PhytochemicalConstituentCollection forCompoundName(CompoundName compoundName);
}
