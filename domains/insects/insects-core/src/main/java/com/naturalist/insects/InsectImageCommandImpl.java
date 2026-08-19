package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

/**
 * Entity-level command adapter for {@link InsectImage}. Aggregate-level
 * orchestration (writes spanning multiple repositories) does not belong here —
 * see {@link InsectCommand}.
 */
@DomainService
class InsectImageCommandImpl
        extends AbstractEntityCommand<InsectImageId, InsectImage, InsectRepository.ImageRepository>
        implements InsectCommand.ImageCommand {

    InsectImageCommandImpl(InsectRepository.ImageRepository repository) {
        super(repository);
    }
}
