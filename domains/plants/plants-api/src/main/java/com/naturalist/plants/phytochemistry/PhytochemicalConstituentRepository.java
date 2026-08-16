package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityRepository;
import com.naturalist.plants.PlantRankName;

import java.util.List;

class PhytochemicalConstituentRepository {
    protected interface PhytochemicalConstituentEntityRepository
            extends EntityRepository<PhytochemicalConstituentName, PhytochemicalConstituent> {

        /**
         * All constituents recorded for a given plant.
         */
        List<PhytochemicalConstituent> getByPlantName(PlantRankName plantName);

        /**
         * All constituents that reference a given compound — the cross-domain
         * reverse lookup (which plants are known to produce this compound).
         */
        List<PhytochemicalConstituent> getByCompoundName(CompoundName compoundName);
    }
}
