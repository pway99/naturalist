package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

@DomainService
class OrderCommandImpl
        extends AbstractEntityCommand<InsectOrderName, InsectOrder, InsectRepository.OrderRepository>
        implements InsectCommand.OrderCommand {

    OrderCommandImpl(InsectRepository.OrderRepository repository) {
        super(repository);
    }
}
