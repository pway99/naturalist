package com.naturalist.naturalist;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistEntityCollections.NaturalistCredentialCollection;

import java.util.Set;

@DomainService
class NaturalistCredentialQueryImpl
        extends AbstractEntityQuery<
        NaturalistName,
        NaturalistCredential,
        NaturalistCredentialCollection,
        NaturalistRepository.CredentialRepository>
        implements NaturalistCredentialQuery {

    NaturalistCredentialQueryImpl(NaturalistRepository.CredentialRepository repository) {
        super(repository);
    }

    @Override
    public NaturalistCredentialCollection findByNameSet(Set<NaturalistName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return NaturalistCredentialCollection.of(repository().getByEntityNameSet(names));
    }
}
