package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;

class FeatureAssignmentCommandImpl
        extends AbstractEntityCommand<InsectFeatureAssignmentId, InsectFeatureAssignment,
                InsectRepository.FeatureAssignmentRepository>
        implements InsectCommand.FeatureAssignmentCommand {

    FeatureAssignmentCommandImpl(InsectRepository.FeatureAssignmentRepository repository) {
        super(repository);
    }
}
