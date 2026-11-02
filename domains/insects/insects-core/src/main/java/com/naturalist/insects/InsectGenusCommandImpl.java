package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

@DomainService
class InsectGenusCommandImpl
        extends AbstractEntityCommand<InsectGenusName, InsectGenusInfo, InsectRepository.GenusRepository>
        implements InsectCommand.GenusCommand {

    InsectGenusCommandImpl(InsectRepository.GenusRepository repository) {
        super(repository);
    }
}
