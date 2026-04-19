package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;

import java.util.Set;

// Claude just hacking things together to see how they work, needs updates
class ImageQueryImpl extends AbstractEntityQuery<InsectImageId, InsectImageName, InsectImage, InsectEntityCollections.ImageCollection> implements InsectQuery.ImageQuery {
    final InsectRepository.ImageRepository imageRepository;

    ImageQueryImpl(InsectRepository.ImageRepository imageRepository) {
        super(imageRepository);
        this.imageRepository = imageRepository;
    }

    @Override
    public InsectEntityCollections.ImageCollection forSpeciesName(InsectSpeciesName speciesName) {
        return new InsectEntityCollections.ImageCollection(imageRepository.getBySpeciesName(speciesName));
    }

    @Override
    public InsectEntityCollections.ImageCollection forSpeciesId(InsectSpeciesId speciesId) {
        return new InsectEntityCollections.ImageCollection(imageRepository.getBySpeciesId(speciesId));
    }

    @Override
    public InsectEntityCollections.ImageCollection findByNameSet(Set<InsectImageName> insectImageNames) {
        return null;
    }

    @Override
    public InsectEntityCollections.ImageCollection findByIdSet(Set<InsectImageId> insectImageIds) {
        return null;
    }
}
