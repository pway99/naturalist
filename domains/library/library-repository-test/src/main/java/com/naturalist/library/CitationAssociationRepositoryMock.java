package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

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
        MethodObserver getByCitationName = observer().forMethod("getByCitationName");
        getByCitationName.arguments(i -> i.entityName(citationName, "citationName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(a -> a.citationName().equals(citationName))
                .toList();
        getByCitationName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<CitationAssociation> getBySubject(EntityRef subject) {
        MethodObserver getBySubject = observer().forMethod("getBySubject");
        getBySubject.arguments(i -> i.valueObject(subject, "subject"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(a -> a.subject().equals(subject))
                .toList();
        getBySubject.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<CitationAssociation> getBySubjects(Set<EntityRef> subjects) {
        MethodObserver getBySubjects = observer().forMethod("getBySubjects");
        getBySubjects.arguments(i -> i.observableCollection(subjects, "subjects"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(a -> subjects.contains(a.subject()))
                .toList();
        getBySubjects.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
