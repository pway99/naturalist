package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Set;

@MockDomainService
class CitationAssociationRepositoryMock
        extends AbstractTestEntityRepository<CitationAssociationId, CitationAssociation, CitationAssociationTestEntitySource>
        implements CitationAssociationRepository {

    CitationAssociationRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<CitationAssociation> getByCitationName(CitationName citationName) {
        observer().arguments("getByCitationName",
                        i -> i.entityName(citationName, "citationName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.citationName().equals(citationName))
                .toList();
    }

    @Override
    public List<CitationAssociation> getBySubject(EntityRef subject) {
        observer().arguments("getBySubject",
                        i -> i.valueObject(subject, "subject"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.subject().equals(subject))
                .toList();
    }

    @Override
    public List<CitationAssociation> getBySubjects(Set<EntityRef> subjects) {
        observer().arguments("getBySubjects",
                        i -> i.observableCollection(subjects, "subjects"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> subjects.contains(a.subject()))
                .toList();
    }
}
