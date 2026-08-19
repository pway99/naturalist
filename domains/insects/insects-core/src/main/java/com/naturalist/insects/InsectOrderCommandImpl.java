package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

@DomainService
class InsectOrderCommandImpl
        extends AbstractEntityCommand<InsectOrderName, InsectOrder, InsectRepository.OrderRepository>
        implements InsectCommand.OrderCommand {

    InsectOrderCommandImpl(InsectRepository.OrderRepository repository) {
        super(repository);
    }
}
