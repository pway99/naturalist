package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;

import java.util.Set;

class ImageQueryImpl
        extends AbstractEntityQuery<
        InsectImageId,
        InsectImage,
        ImageCollection,
        InsectRepository.ImageRepository>
        implements InsectQuery.ImageQuery {

    ImageQueryImpl(InsectRepository.ImageRepository repository) {
        super(repository);
    }

    @Override
    public ImageCollection findByNameSet(Set<InsectImageId> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public ImageCollection forSpeciesName(InsectSpeciesName speciesName) {
        observer().arguments("forSpeciesName", i -> i.entityName(speciesName, "speciesName"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getBySpeciesName(speciesName));
    }
}
