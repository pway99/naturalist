package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.taxonomy.OrganismFeatureAssignment;

@DomainService
class InsectFeatureAssignmentCommandImpl
        extends AbstractEntityCommand<InsectFeatureAssignmentId,
                OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>,
                InsectRepository.FeatureAssignmentRepository>
        implements InsectCommand.FeatureAssignmentCommand {

    InsectFeatureAssignmentCommandImpl(InsectRepository.FeatureAssignmentRepository repository) {
        super(repository);
    }
}
