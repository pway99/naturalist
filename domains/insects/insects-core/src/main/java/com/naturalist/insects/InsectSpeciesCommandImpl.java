package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

/**
 * Entity-level command adapter for {@link InsectSpeciesInfo}. Aggregate-level
 * orchestration (writes spanning multiple repositories) does not belong here —
 * see {@link InsectCommand}.
 */
@DomainService
class InsectSpeciesCommandImpl
        extends AbstractEntityCommand<InsectSpeciesName, InsectSpeciesInfo, InsectRepository.SpeciesRepository>
        implements InsectCommand.SpeciesCommand {

    InsectSpeciesCommandImpl(InsectRepository.SpeciesRepository repository) {
        super(repository);
    }
}
