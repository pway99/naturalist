package com.naturalist.library;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlossaryTermQueryImplTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    GlossaryTermRepository repository = new GlossaryTermRepositoryMock(nte);
    GlossaryTermQuery glossaryTermQuery = new GlossaryTermQueryImpl(repository);

    @Test
    void getByNameReturnsTheTerm() {
        var result = glossaryTermQuery.getByName(TestLibraryIdentifiers.GlossaryTerms.Conspicuous);

        assertThat(result).isPresent();
        assertThat(result.get().term()).isEqualTo("Conspicuous");
    }

    @Test
    void findByNameSetReturnsBothTerms() {
        Set<GlossaryTermName> names = Set.of(
                TestLibraryIdentifiers.GlossaryTerms.Conspicuous,
                TestLibraryIdentifiers.GlossaryTerms.Diagnostic);

        GlossaryTermCollection collection = glossaryTermQuery.findByNameSet(names);

        assertThat(collection.size()).isEqualTo(2);
        assertThat(collection.stream().map(t -> t.name()))
                .containsExactlyInAnyOrderElementsOf(names);
    }
}
