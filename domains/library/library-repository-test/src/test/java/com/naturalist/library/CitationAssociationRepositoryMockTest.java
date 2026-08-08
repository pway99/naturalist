package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectsDomain;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CitationAssociationRepositoryMockTest implements CitationAssociationEntityRepositoryTest {
    @Override
    public CitationAssociationRepository repository() {
        return new CitationAssociationRepositoryMock(db);
    }

    /**
     * {@code CitationAssociation}'s key is a freshly-minted {@code CitationAssociationId}
     * on every call site that builds one (see {@code InsectIdentificationCommand}), so
     * its real identity for dedup purposes is the {@code citationName+subject} unique
     * constraint declared by {@link CitationAssociationTestEntitySource}, not the id.
     * {@link EntityRepository#save} must resolve that: a second save colliding only on
     * the unique constraint updates the existing row in place -- same id, new note --
     * rather than throwing {@code UniqueConstraintException} or silently creating a
     * second row for the same {@code (citationName, subject)} pair.
     */
    @Test
    void save_uniqueConstraintMatch_updatesExistingRowInsteadOfInsertingADuplicate() {
        var citationName = CitationName.of("eol-battus-philenor-130502");
        var subject = new EntityRef(new InsectsDomain(), InsectOrderName.of("unobtainium-save-target"));

        var first = new CitationAssociation(
                CitationAssociationId.create(), citationName, subject, "first note");
        repository().save(first);

        var second = new CitationAssociation(
                CitationAssociationId.create(), citationName, subject, "second note");
        repository().save(second);

        var stored = repository().getBySubject(subject);
        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst().note()).isEqualTo("second note");
        assertThat(stored.getFirst().id()).isEqualTo(first.id());
    }
}
