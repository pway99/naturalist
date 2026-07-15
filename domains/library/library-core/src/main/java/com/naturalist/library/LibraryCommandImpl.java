package com.naturalist.library;

import com.naturalist.observability.Observer;

class LibraryCommandImpl implements LibraryCommand {

    private final CitationCommand citationCommand;
    private final CitationAssociationCommand citationAssociationCommand;

    LibraryCommandImpl(CitationCommand citationCommand,
                       CitationAssociationCommand citationAssociationCommand) {
        Observer.forClass(LibraryCommandImpl.class).arguments("constructor", i -> i
                        .notNull(citationCommand, "citationCommand")
                        .notNull(citationAssociationCommand, "citationAssociationCommand"))
                .throwWhenInvalid();
        this.citationCommand = citationCommand;
        this.citationAssociationCommand = citationAssociationCommand;
    }

    @Override
    public CitationCommand citations() {
        return citationCommand;
    }

    @Override
    public CitationAssociationCommand citationAssociations() {
        return citationAssociationCommand;
    }
}
