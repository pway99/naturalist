package com.naturalist.plants.cultivar;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantName;

import java.util.List;

public class CultivarEntityRepositoryMock
        extends AbstractTestEntityRepository<CultivarName, Cultivar, CultivarTestEntitySource>
        implements CultivarRepository.CultivarEntityRepository {

    protected CultivarEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<CultivarName> getAllCultivarNames() {
        return testEntitySource().entityStream()
                .map(Cultivar::name)
                .toList();
    }

    @Override
    public List<Cultivar> getByPlantName(PlantName plantName) {
        return testEntitySource().entityStream()
                .filter(c -> c.plantName().equals(plantName))
                .toList();
    }
}
