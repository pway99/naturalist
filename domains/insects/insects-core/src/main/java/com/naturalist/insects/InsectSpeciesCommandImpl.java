package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

/**
 * Entity-level command adapter for {@link InsectSpecies}. Aggregate-level
 * orchestration (writes spanning multiple repositories) does not belong here —
 * see {@link InsectCommand}.
 */
@DomainService
class InsectSpeciesCommandImpl
        extends AbstractEntityCommand<InsectSpeciesName, InsectSpecies, InsectRepository.SpeciesRepository>
        implements InsectCommand.SpeciesCommand {

    InsectSpeciesCommandImpl(InsectRepository.SpeciesRepository repository) {
        super(repository);
    }
}
