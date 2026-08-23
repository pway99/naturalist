package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

@DomainService
class CitationCommandImpl
        extends AbstractEntityCommand<CitationName, Citation, CitationRepository>
        implements LibraryCommand.CitationCommand {

    CitationCommandImpl(CitationRepository repository) {
        super(repository);
    }
}
