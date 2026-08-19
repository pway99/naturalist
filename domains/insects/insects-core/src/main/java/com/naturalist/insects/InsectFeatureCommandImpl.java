package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;

class InsectFeatureCommandImpl
        extends AbstractEntityCommand<InsectFeatureId, InsectFeature,
                InsectRepository.FeatureRepository>
        implements InsectCommand.FeatureCommand {

    InsectFeatureCommandImpl(InsectRepository.FeatureRepository repository) {
        super(repository);
    }
}
