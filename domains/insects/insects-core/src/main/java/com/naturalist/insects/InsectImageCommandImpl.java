package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

/**
 * Entity-level command adapter for {@link OrganismImage}. Aggregate-level
 * orchestration (writes spanning multiple repositories) does not belong here —
 * see {@link InsectCommand}.
 */
@DomainService
class InsectImageCommandImpl
        extends AbstractEntityCommand<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, InsectRepository.ImageRepository>
        implements InsectCommand.ImageCommand {

    InsectImageCommandImpl(InsectRepository.ImageRepository repository) {
        super(repository);
    }
}
