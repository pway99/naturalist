package com.naturalist.library;

import com.naturalist.data.AbstractEntityCommand;

class CitationAssociationCommandImpl
        extends AbstractEntityCommand<CitationAssociationId, CitationAssociation,
                CitationAssociationRepository>
        implements LibraryCommand.CitationAssociationCommand {

    CitationAssociationCommandImpl(CitationAssociationRepository repository) {
        super(repository);
    }
}
