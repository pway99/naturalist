package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

@DomainService
class InsectFamilyCommandImpl
        extends AbstractEntityCommand<InsectFamilyName, InsectFamily, InsectRepository.FamilyRepository>
        implements InsectCommand.FamilyCommand {

    InsectFamilyCommandImpl(InsectRepository.FamilyRepository repository) {
        super(repository);
    }
}
