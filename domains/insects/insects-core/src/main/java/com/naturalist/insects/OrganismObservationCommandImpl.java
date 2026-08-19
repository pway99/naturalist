package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.observation.OrganismObservation;

class OrganismObservationCommandImpl
        extends AbstractEntityCommand<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>, InsectRepository.FieldObservationRepository>
        implements InsectCommand.FieldObservationCommand {

    OrganismObservationCommandImpl(InsectRepository.FieldObservationRepository repository) {
        super(repository);
    }
}
