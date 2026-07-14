package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

@DomainService
class GenusCommandImpl
        extends AbstractEntityCommand<InsectGenusName, InsectGenus, InsectRepository.GenusRepository>
        implements InsectCommand.GenusCommand {

    GenusCommandImpl(InsectRepository.GenusRepository repository) {
        super(repository);
    }
}
