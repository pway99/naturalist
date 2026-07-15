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
 */
public interface LibraryCommand {

    CitationCommand citations();

    CitationAssociationCommand citationAssociations();

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
