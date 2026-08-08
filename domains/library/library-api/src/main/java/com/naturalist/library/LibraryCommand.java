package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.EntityCommand;

/**
 * Namespace command for the library bounded context — the single discoverable
 * entry point for mutating library data. Symmetric write-side analogue of
 * {@link CitationQuery} and {@link CitationAssociationQuery}.
 *
 * <p>Nested commands scope to a single entity each:
 * <ul>
 *   <li>{@link CitationCommand} — {@link Citation} mutations.</li>
 *   <li>{@link CitationAssociationCommand} — {@link CitationAssociation} mutations.</li>
 * </ul>
 *
 * <p>{@link #attributeCitation} is the coordinated entry point above those two:
 * it persists a {@link CitationAttribution} — citation plus subject plus note —
 * as a single library-owned transaction, so a caller never has to reason about
 * insert-vs-update or citation-then-association ordering itself. Idempotency for
 * both writes is the library domain's responsibility, not the caller's.
 */
public interface LibraryCommand {

    CitationCommand citations();

    CitationAssociationCommand citationAssociations();

    /**
     * Attaches {@code attribution.citation()} to {@code attribution.subject()},
     * atomically. Idempotent: re-attributing the same citation to the same
     * subject is a no-op past the first call — it never throws a constraint
     * exception for that reason.
     */
    void attributeCitation(CitationAttribution attribution);

    /**
     * Entity-level command surface for {@link Citation}.
     */
    interface CitationCommand extends EntityCommand<CitationName, Citation> {
    }

    /**
     * Entity-level command surface for {@link CitationAssociation}.
     */
    interface CitationAssociationCommand
            extends EntityCommand<CitationAssociationId, CitationAssociation> {
    }
}
