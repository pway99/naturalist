package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

@DomainService
class InsectFeatureCommandImpl
        extends AbstractEntityCommand<InsectFeatureId, InsectFeature,
                InsectRepository.FeatureRepository>
        implements InsectCommand.FeatureCommand {

    InsectFeatureCommandImpl(InsectRepository.FeatureRepository repository) {
        super(repository);
    }
}
