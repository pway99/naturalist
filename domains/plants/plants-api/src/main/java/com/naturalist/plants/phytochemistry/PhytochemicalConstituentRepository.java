package com.naturalist.plants.phytochemistry;

import com.naturalist.data.EntityRepository;
import com.naturalist.plants.PlantRankName;
import com.naturalist.chemistry.compound.CompoundName;

import java.util.List;

/**
 * Persistence port for the phytochemistry sub-context. N=1 collapse (ADR-020): a top-level
 * package-private interface rather than a namespace class, since the sub-context
 * holds a single entity.
 */
interface PhytochemicalConstituentRepository extends EntityRepository<PhytochemicalConstituentName, PhytochemicalConstituent> {

    List<PhytochemicalConstituent> getByPlantName(PlantRankName plantRankName);
    List<PhytochemicalConstituent> getByCompoundName(CompoundName compoundName);
}
