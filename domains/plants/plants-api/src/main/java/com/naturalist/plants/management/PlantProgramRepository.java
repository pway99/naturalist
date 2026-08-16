package com.naturalist.plants.management;

import com.naturalist.data.EntityRepository;
import com.naturalist.plants.PlantRankName;

import java.util.List;

/**
 * Persistence port for the management sub-context. N=1 collapse (ADR-020): a top-level
 * package-private interface rather than a namespace class, since the sub-context
 * holds a single entity.
 */
interface PlantProgramRepository extends EntityRepository<PlantProgramName, PlantProgram> {

    List<PlantProgram> getByPlantName(PlantRankName plantRankName);
}
