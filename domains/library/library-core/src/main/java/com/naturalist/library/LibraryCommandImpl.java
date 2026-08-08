package com.naturalist.library;

import com.naturalist.observability.Observer;

class LibraryCommandImpl implements LibraryCommand {

    private final CitationCommand citationCommand;
    private final CitationAssociationCommand citationAssociationCommand;
    private final CitationAttributionTransaction citationAttributionTransaction;

    LibraryCommandImpl(CitationCommand citationCommand,
                       CitationAssociationCommand citationAssociationCommand,
                       CitationAttributionTransaction citationAttributionTransaction) {
        Observer.forClass(LibraryCommandImpl.class).arguments("constructor", i -> i
                        .notNull(citationCommand, "citationCommand")
                        .notNull(citationAssociationCommand, "citationAssociationCommand")
                        .notNull(citationAttributionTransaction, "citationAttributionTransaction"))
                .throwWhenInvalid();
        this.citationCommand = citationCommand;
        this.citationAssociationCommand = citationAssociationCommand;
        this.citationAttributionTransaction = citationAttributionTransaction;
    }

    @Override
    public CitationCommand citations() {
        return citationCommand;
    }

    @Override
    public CitationAssociationCommand citationAssociations() {
        return citationAssociationCommand;
    }

    @Override
    public void attributeCitation(CitationAttribution attribution) {
        citationAttributionTransaction.execute(attribution);
    }
}
