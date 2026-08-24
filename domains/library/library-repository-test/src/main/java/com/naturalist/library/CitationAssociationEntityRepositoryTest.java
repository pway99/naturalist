package com.naturalist.library;

import com.naturalist.RandomValue;
import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectsDomain;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

interface CitationAssociationEntityRepositoryTest
        extends EntityRepositoryTest<CitationAssociationId, CitationAssociation> {

    @Override
    CitationAssociationRepository repository();

    @Override
    default TestEntitySource<CitationAssociationId, CitationAssociation> source() {
        return db.getNamed(CitationAssociationTestEntitySource.class);
    }

    @Override
    default CitationAssociationId notFoundName() {
        return TestLibraryIdentifiers.Citations.Associations.NotFound.id;
    }

    @Override
    default List<CitationAssociationId> knownEntityNames() {
        return List.of(
            TestLibraryIdentifiers.Citations.Associations.EolLepidopteraOnLepidoptera,
                TestLibraryIdentifiers.Citations.Associations.EolSwallowtailOnPapilionidae);
    }

    @Override
    default CitationAssociation newEntity() {
        return new CitationAssociation(
                CitationAssociationId.create(),
                TestLibraryIdentifiers.Citations.EolGreenLacewing,
                new EntityRef(new InsectsDomain(), InsectOrderName.of("neuroptera")),
                RandomValue.string());
    }

    @Override
    default CitationAssociation ghostEntity() {
        return new CitationAssociation(
                CitationAssociationId.create(),
                TestLibraryIdentifiers.Citations.EolGreenLacewing,
                new EntityRef(new InsectsDomain(), InsectOrderName.of("neuroptera")),
                null);
    }

    @Override
    default CitationAssociation modifiedEntity(CitationAssociation original) {
        return original.withNote(RandomValue.string());
    }

    // =========================================================================
    // getByCitationName
    // =========================================================================

    @Test
    default void getByCitationName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByCitationName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("citationName");
    }

    @Test
    default void getByCitationName_unknownName_returnsEmpty() {
        assertThat(repository().getByCitationName(
                TestLibraryIdentifiers.Citations.NotFound.name))
                .isEmpty();
    }

    @Test
    default void getByCitationName_knownName_returnsAssociations() {
        List<CitationAssociation> result = repository().getByCitationName(
                CitationName.of("eol-battus-philenor-130502"));

        assertThat(result).hasSize(1);
    }

    // =========================================================================
    // getBySubject
    // =========================================================================

    @Test
    default void getBySubject_rejectsNull() {
        assertThatThrownBy(() -> repository().getBySubject(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("subject");
    }

    @Test
    default void getBySubject_unknownSubject_returnsEmpty() {
        EntityRef unknown = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("zygentoma"));
        assertThat(repository().getBySubject(unknown)).isEmpty();
    }

    @Test
    default void getBySubject_knownSubject_returnsAssociations() {
        EntityRef lepidoptera = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("lepidoptera"));

        List<CitationAssociation> result = repository().getBySubject(lepidoptera);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().citationName().value())
            .isEqualTo("eol-lepidoptera-747");
    }

    // =========================================================================
    // getBySubjects
    // =========================================================================

    @Test
    default void getBySubjects_rejectsNull() {
        assertThatThrownBy(() -> repository().getBySubjects(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("subjects");
    }

    @Test
    default void getBySubjects_noneKnown_returnsEmpty() {
        EntityRef unknown = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("zygentoma"));

        assertThat(repository().getBySubjects(Set.of(unknown))).isEmpty();
    }

    @Test
    default void getBySubjects_partialMatch_returnsOnlyKnownSubjects() {
        EntityRef lepidoptera = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("lepidoptera"));
        EntityRef papilionidae = new EntityRef(
                new InsectsDomain(), InsectFamilyName.of("papilionidae"));
        EntityRef unknown = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("zygentoma"));

        List<CitationAssociation> result = repository().getBySubjects(
                Set.of(lepidoptera, papilionidae, unknown));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CitationAssociation::subject)
                .containsExactlyInAnyOrder(lepidoptera, papilionidae);
    }
}
