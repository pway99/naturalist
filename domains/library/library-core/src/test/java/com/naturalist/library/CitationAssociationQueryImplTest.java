package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectsDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CitationAssociationQueryImplTest {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    CitationAssociationRepositoryMock repository = new CitationAssociationRepositoryMock(db);
    CitationAssociationQueryImpl query = new CitationAssociationQueryImpl(repository);

    @Test
    void findByCitationName_rejectsNull() {
        assertThatThrownBy(() -> query.findByCitationName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("citationName");
    }

    @Test
    void findByCitationName_knownName_returnsCollection() {
        CitationAssociationCollection result = query.findByCitationName(
                CitationName.of("eol-battus-philenor-130502"));

        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    void findBySubject_rejectsNull() {
        assertThatThrownBy(() -> query.findBySubject(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void findBySubject_knownSubject_returnsCollection() {
        EntityRef lepidoptera = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("lepidoptera"));

        CitationAssociationCollection result = query.findBySubject(lepidoptera);

        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    void findBySubject_unknownSubject_returnsEmpty() {
        EntityRef unknown = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("zygentoma"));

        CitationAssociationCollection result = query.findBySubject(unknown);

        assertThat(result.isEmpty()).isTrue();
    }
}
