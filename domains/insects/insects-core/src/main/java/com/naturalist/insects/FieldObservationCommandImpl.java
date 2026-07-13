package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;

class FieldObservationCommandImpl
        extends AbstractEntityCommand<FieldObservationId, FieldObservation, InsectRepository.FieldObservationRepository>
        implements InsectCommand.FieldObservationCommand {

    FieldObservationCommandImpl(InsectRepository.FieldObservationRepository repository) {
        super(repository);
    }
}
