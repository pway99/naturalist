package com.naturalist.insects;

import com.naturalist.data.EntityCommand;

/**
 * Namespace command for the insects bounded context — the single discoverable entry
 * point for mutating insect catalog data. Symmetric write-side analogue of
 * {@link InsectQuery}.
 *
 * <p>Nested commands scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesCommand} — {@link InsectSpecies} mutations.</li>
 *   <li>{@link ImageCommand} — {@link InsectImage} mutations.</li>
 * </ul>
 *
 * <p><b>Scope.</b> The nested interfaces are <i>entity-level</i> commands — each
 * extends {@link EntityCommand} and operates on a single repository. Aggregate-level
 * write orchestration (e.g. inserting a species together with its images in one
 * transaction) does not belong on the nested types; it belongs as a top-level method
 * on this namespace, in a separate aggregate-command port, or in controller
 * orchestration. The current pilot ships entity commands only.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * insectCommand.species().insert(newSpecies);
 * insectCommand.images().update(modifiedImage);
 * }</pre>
 *
 * <p>Family and genus commands are not part of the pilot — add them when a write
 * surface for those entities becomes a concrete requirement.
 */
public interface InsectCommand {

    SpeciesCommand species();

    ImageCommand images();

    FieldObservationCommand fieldObservations();

    /**
     * Entity-level command surface for {@link InsectSpecies}.
     */
    interface SpeciesCommand extends EntityCommand<InsectSpeciesName, InsectSpecies> {
    }

    /**
     * Entity-level command surface for {@link InsectImage}.
     */
    interface ImageCommand extends EntityCommand<InsectImageId, InsectImage> {
    }

    /**
     * Entity-level command surface for {@link FieldObservation}.
     */
    interface FieldObservationCommand
            extends EntityCommand<FieldObservationId, FieldObservation> {
    }
}
