package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;

@DomainService
class InsectFeatureAssignmentCommandImpl
        extends AbstractEntityCommand<InsectFeatureAssignmentId, InsectFeatureAssignment,
                InsectRepository.FeatureAssignmentRepository>
        implements InsectCommand.FeatureAssignmentCommand {

    InsectFeatureAssignmentCommandImpl(InsectRepository.FeatureAssignmentRepository repository) {
        super(repository);
    }
}
