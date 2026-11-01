package com.naturalist.insects;

import com.naturalist.data.Transaction;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

/**
 * Persists a {@link PhotoAddition} aggregate atomically — inserts the
 * optional {@link OrganismObservation} before the {@link OrganismImage} so the
 * image's {@code observationId} FK is satisfied on insert.
 *
 * <p>Writes through the {@link InsectEntityCommands} bundle, not
 * {@link InsectCommand}, so use cases composed into the command namespace can
 * depend on this transaction without a constructor cycle.
 */
@DomainService
class InsectAddPhotoTransaction extends Transaction<PhotoAddition> {

    private final InsectEntityCommands commands;

    InsectAddPhotoTransaction(InsectEntityCommands commands) {
        this.commands = commands;
    }

    @Override
    protected void doExecute(PhotoAddition addition) {
        if (addition.observation() != null) {
            commands.observations().insert(addition.observation());
        }
        commands.images().insert(addition.image());
    }
}
