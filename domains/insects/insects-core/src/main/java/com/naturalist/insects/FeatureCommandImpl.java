package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;

class FeatureCommandImpl
        extends AbstractEntityCommand<InsectFeatureId, InsectFeature,
                InsectRepository.FeatureRepository>
        implements InsectCommand.FeatureCommand {

    FeatureCommandImpl(InsectRepository.FeatureRepository repository) {
        super(repository);
    }
}
