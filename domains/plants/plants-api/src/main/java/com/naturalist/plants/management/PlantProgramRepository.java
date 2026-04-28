package com.naturalist.plants.management;

import com.naturalist.data.EntityRepository;
import com.naturalist.plants.PlantName;

import java.util.List;

class PlantProgramRepository {
    protected interface PlantProgramEntityRepository
            extends EntityRepository<PlantProgramName, PlantProgram> {

        List<PlantProgramName> getAllPlantProgramNames();

        List<PlantProgram> getByPlantName(PlantName plantName);
    }
}
