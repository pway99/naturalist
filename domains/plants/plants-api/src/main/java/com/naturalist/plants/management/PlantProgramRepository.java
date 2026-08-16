package com.naturalist.plants.management;

import com.naturalist.data.EntityRepository;
import com.naturalist.plants.PlantRankName;

import java.util.List;

class PlantProgramRepository {
    protected interface PlantProgramEntityRepository
            extends EntityRepository<PlantProgramName, PlantProgram> {

        List<PlantProgram> getByPlantName(PlantRankName plantName);
    }
}
