package com.naturalist.insects;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

@DomainService
class InsectCommandImpl implements InsectCommand {

    private final InsectEntityCommands commands;

    InsectCommandImpl(InsectEntityCommands commands) {
        Observer.forClass(InsectCommandImpl.class).arguments("constructor", i -> i
                        .notNull(commands, "commands"))
                .throwWhenInvalid();
        this.commands = commands;
    }

    @Override
    public SpeciesCommand species() {
        return commands.species();
    }

    @Override
    public ImageCommand images() {
        return commands.images();
    }

    @Override
    public ObservationCommand observations() {
        return commands.observations();
    }

    @Override
    public OrderCommand orders() {
        return commands.orders();
    }

    @Override
    public FamilyCommand families() {
        return commands.families();
    }

    @Override
    public GenusCommand genera() {
        return commands.genera();
    }

    @Override
    public FeatureCommand features() {
        return commands.features();
    }

    @Override
    public FeatureAssignmentCommand featureAssignments() {
        return commands.featureAssignments();
    }
}
