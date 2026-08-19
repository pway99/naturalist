package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.observation.OrganismObservation;

class InsectObservationCommandImpl
        extends AbstractEntityCommand<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>, InsectRepository.ObservationRepository>
        implements InsectCommand.ObservationCommand {

    InsectObservationCommandImpl(InsectRepository.ObservationRepository repository) {
        super(repository);
    }
}
