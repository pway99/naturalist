package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CitationQueryImplTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    CitationRepository repository = new CitationRepositoryMock(nte);
    CitationQuery citationQuery = new CitationQueryImpl(repository);

    @Test
    void findByNameSet_happyPath() {
        Set<CitationName> names = Set.of(
                TestLibraryIdentifiers.Citations.EolSwallowtail,
                TestLibraryIdentifiers.Citations.EolGreenLacewing
        );

        CitationCollection collection = citationQuery.findByNameSet(names);

        assertThat(collection).isNotNull();
        assertThat(collection.size()).isEqualTo(2);
        assertThat(collection.stream().map(c -> c.name()))
                .containsExactlyInAnyOrderElementsOf(names);
    }
}
