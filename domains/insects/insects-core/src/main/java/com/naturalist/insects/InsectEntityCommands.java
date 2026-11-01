package com.naturalist.insects;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

/**
 * The eight insect entity write ports as one injectable unit, so transactions and
 * use cases can take them without depending on {@link InsectCommand} (whose
 * implementation will depend on the use cases).
 */
@DomainService
final class InsectEntityCommands {

    private final InsectCommand.SpeciesCommand speciesCommand;
    private final InsectCommand.ImageCommand imageCommand;
    private final InsectCommand.ObservationCommand observationCommand;
    private final InsectCommand.OrderCommand orderCommand;
    private final InsectCommand.FamilyCommand familyCommand;
    private final InsectCommand.GenusCommand genusCommand;
    private final InsectCommand.FeatureCommand featureCommand;
    private final InsectCommand.FeatureAssignmentCommand featureAssignmentCommand;

    InsectEntityCommands(InsectCommand.SpeciesCommand speciesCommand,
                         InsectCommand.ImageCommand imageCommand,
                         InsectCommand.ObservationCommand observationCommand,
                         InsectCommand.OrderCommand orderCommand,
                         InsectCommand.FamilyCommand familyCommand,
                         InsectCommand.GenusCommand genusCommand,
                         InsectCommand.FeatureCommand featureCommand,
                         InsectCommand.FeatureAssignmentCommand featureAssignmentCommand) {
        Observer.forClass(InsectEntityCommands.class).arguments("constructor", i -> i
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

    InsectCommand.SpeciesCommand species() {
        return speciesCommand;
    }

    InsectCommand.ImageCommand images() {
        return imageCommand;
    }

    InsectCommand.ObservationCommand observations() {
        return observationCommand;
    }

    InsectCommand.OrderCommand orders() {
        return orderCommand;
    }

    InsectCommand.FamilyCommand families() {
        return familyCommand;
    }

    InsectCommand.GenusCommand genera() {
        return genusCommand;
    }

    InsectCommand.FeatureCommand features() {
        return featureCommand;
    }

    InsectCommand.FeatureAssignmentCommand featureAssignments() {
        return featureAssignmentCommand;
    }
}
