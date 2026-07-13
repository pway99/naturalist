package com.naturalist.insects;

import com.naturalist.observability.Observer;

class InsectCommandImpl implements InsectCommand {

    private final SpeciesCommand speciesCommand;
    private final ImageCommand imageCommand;
    private final FieldObservationCommand fieldObservationCommand;

    InsectCommandImpl(SpeciesCommand speciesCommand, ImageCommand imageCommand,
                       FieldObservationCommand fieldObservationCommand) {
        Observer.forClass(InsectCommandImpl.class).arguments("constructor", i -> i
                        .notNull(speciesCommand, "speciesCommand")
                        .notNull(imageCommand, "imageCommand")
                        .notNull(fieldObservationCommand, "fieldObservationCommand"))
                .throwWhenInvalid();
        this.speciesCommand = speciesCommand;
        this.imageCommand = imageCommand;
        this.fieldObservationCommand = fieldObservationCommand;
    }

    @Override
    public SpeciesCommand species() {
        return speciesCommand;
    }

    @Override
    public ImageCommand images() {
        return imageCommand;
    }

    @Override
    public FieldObservationCommand fieldObservations() {
        return fieldObservationCommand;
    }
}
