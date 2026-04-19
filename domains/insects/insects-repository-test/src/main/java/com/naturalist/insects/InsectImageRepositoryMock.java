package com.naturalist.insects;

import com.naturalist.data.AbstractTestNamedEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;

class InsectImageRepositoryMock
        extends AbstractTestNamedEntityRepository<InsectImageName, InsectImage, InsectImageTestEntitySource>
        implements InsectRepository.ImageRepository {

    InsectImageRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectImage> getBySpeciesName(InsectSpeciesName speciesName) {
        return testEntitySource().entityStream()
                .filter(image -> image.insectSpeciesName().equals(speciesName))
                .toList();
    }
}
