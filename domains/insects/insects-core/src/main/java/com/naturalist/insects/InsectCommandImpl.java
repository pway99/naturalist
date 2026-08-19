package com.naturalist.insects;

import com.naturalist.observability.Observer;

class InsectCommandImpl implements InsectCommand {

    private final SpeciesCommand speciesCommand;
    private final ImageCommand imageCommand;
    private final ObservationCommand observationCommand;
    private final OrderCommand orderCommand;
    private final FamilyCommand familyCommand;
    private final GenusCommand genusCommand;
    private final FeatureCommand featureCommand;
    private final FeatureAssignmentCommand featureAssignmentCommand;

    InsectCommandImpl(SpeciesCommand speciesCommand, ImageCommand imageCommand,
                       ObservationCommand observationCommand,
                       OrderCommand orderCommand, FamilyCommand familyCommand,
                       GenusCommand genusCommand, FeatureCommand featureCommand,
                       FeatureAssignmentCommand featureAssignmentCommand) {
        Observer.forClass(InsectCommandImpl.class).arguments("constructor", i -> i
                        .notNull(speciesCommand, "speciesCommand")
                        .notNull(imageCommand, "imageCommand")
                        .notNull(observationCommand, "observationCommand")
                        .notNull(orderCommand, "orderCommand")
                        .notNull(familyCommand, "familyCommand")
                        .notNull(genusCommand, "genusCommand")
                        .notNull(featureCommand, "featureCommand")
                        .notNull(featureAssignmentCommand, "featureAssignmentCommand"))
                .throwWhenInvalid();
        this.speciesCommand = speciesCommand;
        this.imageCommand = imageCommand;
        this.observationCommand = observationCommand;
        this.orderCommand = orderCommand;
        this.familyCommand = familyCommand;
        this.genusCommand = genusCommand;
        this.featureCommand = featureCommand;
        this.featureAssignmentCommand = featureAssignmentCommand;
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
    public ObservationCommand observations() {
        return observationCommand;
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

    @Override
    public FeatureCommand features() {
        return featureCommand;
    }

    @Override
    public FeatureAssignmentCommand featureAssignments() {
        return featureAssignmentCommand;
    }
}
