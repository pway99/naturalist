package com.naturalist.insects;

import com.naturalist.data.Transaction;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

/**
 * Persists a {@link PhotoAddition} aggregate atomically — inserts the
 * optional {@link OrganismObservation} before the {@link OrganismImage} so the
 * image's {@code observationId} FK is satisfied on insert.
 */
public class InsectAddPhotoTransaction extends Transaction<PhotoAddition> {

    private final InsectCommand insectCommand;

    public InsectAddPhotoTransaction(InsectCommand insectCommand) {
        this.insectCommand = insectCommand;
    }

    @Override
    protected void doExecute(PhotoAddition addition) {
        if (addition.observation() != null) {
            insectCommand.observations().insert(addition.observation());
        }
        insectCommand.images().insert(addition.image());
    }
}
