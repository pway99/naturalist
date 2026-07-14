package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

@DomainService
class FamilyCommandImpl
        extends AbstractEntityCommand<InsectFamilyName, InsectFamily, InsectRepository.FamilyRepository>
        implements InsectCommand.FamilyCommand {

    FamilyCommandImpl(InsectRepository.FamilyRepository repository) {
        super(repository);
    }
}
