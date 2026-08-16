package com.naturalist.plants.cultivar;

import com.naturalist.data.EntityRepository;
import com.naturalist.plants.PlantSpeciesName;

import java.util.List;

/**
 * Persistence port for the cultivar sub-context. N=1 collapse (ADR-020): a top-level
 * package-private interface rather than a namespace class, since the sub-context
 * holds a single entity.
 */
interface CultivarRepository extends EntityRepository<CultivarName, Cultivar> {

    List<Cultivar> getByPlantName(PlantSpeciesName plantSpeciesName);
}
