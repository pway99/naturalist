package com.naturalist.library;

import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectsDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CitationAttributionTransaction} is where "citation idempotency owned
 * by the library domain, not the insects command" (the design doc's own
 * words) is actually implemented — the association write goes through
 * {@link LibraryCommand.CitationAssociationCommand#save}, never a caught
 * constraint exception, and the citation write is first-write-wins (a read
 * via {@link CitationRepository#getByName}, not save()) so a later,
 * lower-trust source (e.g. a vision-suggested URL) can never overwrite an
 * existing citation's authority content. These tests exercise: re-attributing
 * is a no-op past the first call, a shared citation can legitimately back
 * more than one subject, and an existing citation's own fields survive a
 * later attribution attempt untouched.
 */
class CitationAttributionTransactionTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    CitationRepository citationRepository = new CitationRepositoryMock(db);
    CitationAssociationRepository citationAssociationRepository = new CitationAssociationRepositoryMock(db);

    LibraryCommand.CitationCommand citationCommand = new CitationCommandImpl(citationRepository);
    LibraryCommand.CitationAssociationCommand citationAssociationCommand =
            new CitationAssociationCommandImpl(citationAssociationRepository);
    CitationAssociationQuery citationAssociationQuery =
            new CitationAssociationQueryImpl(citationAssociationRepository);

    CitationAttributionTransaction transaction =
            new CitationAttributionTransaction(citationRepository, citationCommand, citationAssociationCommand);

    private static OnlineSource citation(String slug) {
        return new OnlineSource(
                CitationName.of(slug),
                new AuthorityReference(new AuthoritySource("test", "Test Authority"),
                        URI.create("https://test.example.com/" + slug)),
                "Test citation " + slug, null, null, null);
    }

    private static EntityRef subject(String slug) {
        return new EntityRef(new InsectsDomain(), InsectOrderName.of(slug));
    }

    @Test
    void execute_newAttribution_persistsCitationAndAssociation() {
        var citation = citation("test-attribution-new");
        var subject = subject("unobtainium-new");

        transaction.execute(new CitationAttribution(citation, subject, "first note"));

        assertThat(citationRepository.getByName(citation.name())).isPresent();
        var associations = citationAssociationQuery.findBySubject(subject);
        assertThat(associations.size()).isEqualTo(1);
        assertThat(associations.stream().findFirst().orElseThrow().note()).isEqualTo("first note");
    }

    @Test
    void execute_sameCitationSameSubjectTwice_createsExactlyOneAssociation() {
        var citation = citation("test-attribution-repeat-subject");
        var subject = subject("unobtainium-repeat");

        transaction.execute(new CitationAttribution(citation, subject, "first note"));
        transaction.execute(new CitationAttribution(citation, subject, "second note"));

        var associations = citationAssociationQuery.findBySubject(subject);
        assertThat(associations.size()).isEqualTo(1);
        assertThat(associations.stream().findFirst().orElseThrow().note()).isEqualTo("second note");
    }

    /**
     * The regression this transaction exists to prevent: a citation slug is
     * deterministic ({@code source id + rank name}), so a later, lower-trust
     * identification (e.g. authority lookup found nothing this run and vision
     * supplied its own, potentially hallucinated, {@code referenceUrl}) can
     * regenerate the exact same {@code CitationName} as an earlier, verified
     * write. That later attempt must not overwrite the stored citation's
     * {@code authorityReference}/{@code title} — assert on the stored
     * citation after the second call, not on the second call's input.
     */
    @Test
    void execute_citationAlreadyExists_leavesStoredCitationUntouchedButStillCreatesAssociation() {
        var slug = "test-attribution-preexisting";
        var verified = citation(slug);
        citationCommand.insert(verified);
        var subject = subject("unobtainium-preexisting");

        var laterLowerTrustCitation = new OnlineSource(
                CitationName.of(slug),
                new AuthorityReference(new AuthoritySource("ref", "Suggested Reference"),
                        URI.create("https://vision.example.com/hallucinated-" + slug)),
                "Hallucinated title for " + slug, null, null, null);

        transaction.execute(new CitationAttribution(laterLowerTrustCitation, subject, "second identification"));

        var stored = citationRepository.getByName(CitationName.of(slug)).orElseThrow();
        assertThat(stored).isEqualTo(verified);
        assertThat(((OnlineSource) stored).authorityReference()).isEqualTo(verified.authorityReference());
        assertThat(stored.title()).isEqualTo(verified.title());

        var associations = citationAssociationQuery.findBySubject(subject);
        assertThat(associations.size()).isEqualTo(1);
        assertThat(associations.stream().findFirst().orElseThrow().note())
                .isEqualTo("second identification");
    }

    @Test
    void execute_sameCitationTwoDifferentSubjects_createsTwoAssociations() {
        var citation = citation("test-attribution-shared-citation");
        var subjectA = subject("unobtainium-subject-a");
        var subjectB = subject("unobtainium-subject-b");

        transaction.execute(new CitationAttribution(citation, subjectA, "note a"));
        transaction.execute(new CitationAttribution(citation, subjectB, "note b"));

        var associations = citationAssociationQuery.findByCitationName(citation.name());
        assertThat(associations.size()).isEqualTo(2);
        assertThat(associations.stream().map(CitationAssociation::subject))
                .containsExactlyInAnyOrder(subjectA, subjectB);
    }
}
