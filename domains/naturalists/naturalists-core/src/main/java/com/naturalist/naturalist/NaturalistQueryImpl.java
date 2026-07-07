package com.naturalist.naturalist;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistEntityCollections.NaturalistCollection;

import java.util.Set;

@DomainService
class NaturalistQueryImpl
        extends AbstractEntityQuery<
        NaturalistName,
        Naturalist,
        NaturalistCollection,
        NaturalistRepository.NaturalistEntityRepository>
        implements NaturalistQuery {

    NaturalistQueryImpl(NaturalistRepository.NaturalistEntityRepository repository) {
        super(repository);
    }

    @Override
    public NaturalistCollection findByNameSet(Set<NaturalistName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return NaturalistCollection.of(repository().getByEntityNameSet(names));
    }
}
