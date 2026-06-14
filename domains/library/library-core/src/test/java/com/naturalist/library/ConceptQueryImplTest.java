package com.naturalist.library;

import com.naturalist.data.NaturalistDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConceptQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    ConceptRepository repository = new ConceptRepositoryMock(db);
    ConceptQuery conceptQuery = new ConceptQueryImpl(repository);

    @Test
    void getByNameReturnsTheConcept() {
        var result = conceptQuery.getByName(TestLibraryIdentifiers.Concepts.Clade);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("What is a clade?");
    }

    @Test
    void findByNameSetReturnsBothConcepts() {
        Set<ConceptName> names = Set.of(
                TestLibraryIdentifiers.Concepts.Clade,
                TestLibraryIdentifiers.Concepts.CladeTaxonomyRelation);

        ConceptCollection collection = conceptQuery.findByNameSet(names);

        assertThat(collection.size()).isEqualTo(2);
        assertThat(collection.stream().map(c -> c.name()))
                .containsExactlyInAnyOrderElementsOf(names);
    }
}
