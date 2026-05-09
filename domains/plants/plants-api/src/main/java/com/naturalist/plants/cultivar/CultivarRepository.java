package com.naturalist.plants.cultivar;

import com.naturalist.data.EntityRepository;
import com.naturalist.plants.PlantName;

import java.util.List;

class CultivarRepository {
    protected interface CultivarEntityRepository
            extends EntityRepository<CultivarName, Cultivar> {

        List<Cultivar> getByPlantName(PlantName plantName);
    }
}
