package com.naturalist.insects;

import com.naturalist.data.AbstractNamedEntityQuery;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;

import java.util.Set;

class ImageQueryImpl
        extends AbstractNamedEntityQuery<
                InsectImageName,
                InsectImage,
                ImageCollection,
                InsectRepository.ImageRepository>
        implements InsectQuery.ImageQuery {

    ImageQueryImpl(InsectRepository.ImageRepository repository) {
        super(repository);
    }

    @Override
    public ImageCollection findByNameSet(Set<InsectImageName> names) {
        observer().arguments("findByNameSet", i -> i.notNull(names, "names"))
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
