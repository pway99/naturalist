package com.naturalist.insects;

import com.naturalist.observability.Observer;

class InsectCommandImpl implements InsectCommand {

    private final SpeciesCommand speciesCommand;
    private final ImageCommand imageCommand;
    private final FieldObservationCommand fieldObservationCommand;
    private final OrderCommand orderCommand;
    private final FamilyCommand familyCommand;
    private final GenusCommand genusCommand;

    InsectCommandImpl(SpeciesCommand speciesCommand, ImageCommand imageCommand,
                       FieldObservationCommand fieldObservationCommand,
                       OrderCommand orderCommand, FamilyCommand familyCommand,
                       GenusCommand genusCommand) {
        Observer.forClass(InsectCommandImpl.class).arguments("constructor", i -> i
                        .notNull(speciesCommand, "speciesCommand")
                        .notNull(imageCommand, "imageCommand")
                        .notNull(fieldObservationCommand, "fieldObservationCommand")
                        .notNull(orderCommand, "orderCommand")
                        .notNull(familyCommand, "familyCommand")
                        .notNull(genusCommand, "genusCommand"))
                .throwWhenInvalid();
        this.speciesCommand = speciesCommand;
        this.imageCommand = imageCommand;
        this.fieldObservationCommand = fieldObservationCommand;
        this.orderCommand = orderCommand;
        this.familyCommand = familyCommand;
        this.genusCommand = genusCommand;
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

    @Override
    public OrderCommand orders() {
        return orderCommand;
    }

    @Override
    public FamilyCommand families() {
        return familyCommand;
    }

    @Override
    public GenusCommand genera() {
        return genusCommand;
    }
}
