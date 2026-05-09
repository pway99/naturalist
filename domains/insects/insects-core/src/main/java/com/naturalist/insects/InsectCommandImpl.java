package com.naturalist.insects;

import com.naturalist.observability.Observer;

class InsectCommandImpl implements InsectCommand {

    private final SpeciesCommand speciesCommand;
    private final ImageCommand imageCommand;

    InsectCommandImpl(SpeciesCommand speciesCommand, ImageCommand imageCommand) {
        Observer.forClass(InsectCommandImpl.class).arguments("constructor", i -> i
                        .notNull(speciesCommand, "speciesCommand")
                        .notNull(imageCommand, "imageCommand"))
                .throwWhenInvalid();
        this.speciesCommand = speciesCommand;
        this.imageCommand = imageCommand;
    }

    @Override
    public SpeciesCommand species() {
        return speciesCommand;
    }

    @Override
    public ImageCommand images() {
        return imageCommand;
    }
}
