package com.naturalist.insects;

import com.naturalist.data.EntityCommand;
import com.naturalist.observation.OrganismObservation;

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
 */
public interface InsectCommand {

    SpeciesCommand species();

    ImageCommand images();

    FieldObservationCommand fieldObservations();

    OrderCommand orders();

    FamilyCommand families();

    GenusCommand genera();

    FeatureCommand features();

    FeatureAssignmentCommand featureAssignments();

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
     * Entity-level command surface for {@link OrganismObservation}.
     */
    interface FieldObservationCommand
            extends EntityCommand<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>> {
    }

    /**
     * Entity-level command surface for {@link InsectOrder}.
     */
    interface OrderCommand extends EntityCommand<InsectOrderName, InsectOrder> {
    }

    /**
     * Entity-level command surface for {@link InsectFamily}.
     */
    interface FamilyCommand extends EntityCommand<InsectFamilyName, InsectFamily> {
    }

    /**
     * Entity-level command surface for {@link InsectGenus}.
     */
    interface GenusCommand extends EntityCommand<InsectGenusName, InsectGenus> {
    }

    /**
     * Entity-level command surface for {@link InsectFeature}.
     */
    interface FeatureCommand extends EntityCommand<InsectFeatureId, InsectFeature> {
    }

    /**
     * Entity-level command surface for {@link InsectFeatureAssignment}.
     */
    interface FeatureAssignmentCommand
            extends EntityCommand<InsectFeatureAssignmentId, InsectFeatureAssignment> {
    }
}
