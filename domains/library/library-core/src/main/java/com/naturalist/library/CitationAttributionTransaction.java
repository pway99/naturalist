package com.naturalist.library;

import com.naturalist.data.Transaction;

/**
 * Persists a {@link CitationAttribution} atomically — the citation and its
 * association to the subject.
 *
 * <p>The citation write is <b>first-write-wins</b>, not {@code save()}: this
 * checks {@link CitationRepository#getByName} — a read, not a caught constraint
 * violation, so this does not reintroduce the exception-driven control flow the
 * plan owner rejected for F1 — and only inserts when no row for that
 * {@code CitationName} already exists. An existing row is either curated
 * fixture data or an earlier verified write, and a later vision-suggested
 * reference is the least trustworthy source in the system: it must never
 * overwrite a citation's {@code authorityReference}/{@code title}. The
 * deterministic slug ({@code source id + rank name}, see
 * {@code InsectIdentificationCommand}) means a later identification of the
 * same rank regenerates the same {@code CitationName} even when this run's
 * authority lookup found nothing and fell back to a vision-suggested URL —
 * without this check, that hallucination-prone fallback would silently and
 * permanently replace a verified reference. Semantics: attribute this
 * citation to this subject; if the citation already exists, attribute the
 * existing one as-is.
 *
 * <p>{@link LibraryCommand.CitationAssociationCommand#save} dedupes the
 * association by the {@code citationName + subject} unique constraint
 * declared on {@code CitationAssociationTestEntitySource} — even though a
 * fresh {@code CitationAssociationId} is minted at every call site that
 * builds one (see {@code InsectIdentificationCommand}), because the
 * repository layer underneath {@code save()} resolves identity by declared
 * unique constraint, not just by primary key. This transaction does not need
 * to know that resolution happens, only that it does — that is what makes it
 * safe to call {@code save()} twice with two different association ids for
 * the same {@code (citationName, subject)} pair and still end up with one
 * row. Unlike the citation, the association's own fields (currently just
 * {@code note}) are safe to let a later call overwrite — re-attributing is
 * meant to refresh the association, only the citation's authority content is
 * the thing that must never be overwritten by a lower-trust source.
 */
class CitationAttributionTransaction extends Transaction<CitationAttribution> {

    private final CitationRepository citationRepository;
    private final LibraryCommand.CitationCommand citationCommand;
    private final LibraryCommand.CitationAssociationCommand citationAssociationCommand;

    CitationAttributionTransaction(CitationRepository citationRepository,
                                   LibraryCommand.CitationCommand citationCommand,
                                   LibraryCommand.CitationAssociationCommand citationAssociationCommand) {
        this.citationRepository = citationRepository;
        this.citationCommand = citationCommand;
        this.citationAssociationCommand = citationAssociationCommand;
    }

    @Override
    protected void doExecute(CitationAttribution attribution) {
        if (citationRepository.getByName(attribution.citation().name()).isEmpty()) {
            citationCommand.insert(attribution.citation());
        }

        var association = new CitationAssociation(
                CitationAssociationId.create(),
                attribution.citation().name(),
                attribution.subject(),
                attribution.note());
        citationAssociationCommand.save(association);
    }
}
