package com.naturalist.insects;

import com.naturalist.data.AbstractNamedEntityQuery;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;

import java.util.Set;

class ImageQueryImpl
        extends AbstractNamedEntityQuery<InsectImageName, InsectImage, ImageCollection>
        implements InsectQuery.ImageQuery {

    private final InsectRepository.ImageRepository imageRepository;

    ImageQueryImpl(InsectRepository.ImageRepository imageRepository) {
        super(imageRepository);
        this.imageRepository = imageRepository;
    }

    @Override
    public ImageCollection findByNameSet(Set<InsectImageName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return ImageCollection.of(imageRepository.getByEntityNameSet(names));
    }

    @Override
    public ImageCollection forSpeciesName(InsectSpeciesName speciesName) {
        observer().arguments("forSpeciesName", i -> i.entityName(speciesName, "speciesName"))
                .throwWhenInvalid();
        return ImageCollection.of(imageRepository.getBySpeciesName(speciesName));
    }
}
