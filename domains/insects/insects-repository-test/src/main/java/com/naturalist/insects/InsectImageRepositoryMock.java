package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Observer;

import java.util.List;

class InsectImageRepositoryMock extends AbstractTestEntityRepository<InsectImageId, InsectImageName, InsectImage, InsectImageTestEntitySource>
    implements InsectRepository.ImageRepository {
    private static final Observer observer = Observer.forClass(InsectImageRepositoryMock.class);

    protected InsectImageRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Observer observer() {
        return observer;
    }

    @Override
    public List<InsectImage> getBySpeciesName(InsectSpeciesName speciesName) {
        return testEntitySource().entityStream()
                .filter(insectImage -> insectImage.insectSpeciesName().equals(speciesName))
                .toList();
    }

    @Override
    public List<InsectImage> getBySpeciesId(InsectSpeciesId speciesId) {
        return testEntitySource().entityStream()
                .filter(insectImage -> insectImage.id().equals(speciesId))
                .toList();
    }
}
