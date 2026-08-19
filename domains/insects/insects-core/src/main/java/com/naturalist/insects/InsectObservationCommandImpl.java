package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;
import com.naturalist.observation.OrganismObservation;

class InsectObservationCommandImpl
        extends AbstractEntityCommand<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>, InsectRepository.InsectObservationRepository>
        implements InsectCommand.InsectObservationCommand {

    InsectObservationCommandImpl(InsectRepository.InsectObservationRepository repository) {
        super(repository);
    }
}
