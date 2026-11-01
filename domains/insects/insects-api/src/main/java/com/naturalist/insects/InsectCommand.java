package com.naturalist.insects;

import com.naturalist.data.EntityCommand;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.taxonomy.OrganismFeatureAssignment;
import org.jspecify.annotations.Nullable;

/**
 * Namespace command for the insects bounded context — the single discoverable entry
 * point for mutating insect catalog data. Symmetric write-side analogue of
 * {@link InsectQuery}.
 *
 * <p>Nested commands scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesCommand} — {@link InsectSpecies} mutations.</li>
 *   <li>{@link ImageCommand} — {@link OrganismImage} mutations.</li>
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

    ObservationCommand observations();

    OrderCommand orders();

    FamilyCommand families();

    GenusCommand genera();

    FeatureCommand features();

    FeatureAssignmentCommand featureAssignments();

    SightingCommand sightings();

    PhotoCommand photos();

    /**
     * Use-case command: a naturalist's field sightings of an insect.
     */
    interface SightingCommand {

        /**
         * Records that {@code naturalist} observed {@code subject} now; blank notes are stored as null.
         */
        InsectObservationId record(InsectRankName subject, NaturalistName naturalist, @Nullable String notes);

        /**
         * Replaces the field notes of the viewer's own observation; a missing observation or one
         * recorded by someone else is refused silently (no exception, no change) so the id alone
         * reveals nothing. Blank notes clear the field.
         */
        void updateNotes(InsectObservationId observationId, NaturalistName viewer, @Nullable String notes);
    }

    /**
     * Entity-level command surface for {@link InsectSpecies}.
     */
    interface SpeciesCommand extends EntityCommand<InsectSpeciesName, InsectSpecies> {
    }

    /**
     * Entity-level command surface for {@link OrganismImage}.
     */
    interface ImageCommand extends EntityCommand<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> {
    }

    /**
     * Entity-level command surface for {@link OrganismObservation}.
     */
    interface ObservationCommand
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
     * Entity-level command surface for {@link OrganismFeatureAssignment}.
     */
    interface FeatureAssignmentCommand
            extends EntityCommand<InsectFeatureAssignmentId,
                    OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> {
    }

    /**
     * Use-case command: adding a photo to a catalog entry through the image store.
     */
    interface PhotoCommand {
        /**
         * Stores the upload, then persists an image at {@code subject} (plus an observation when
         * {@code naturalist} is given) atomically; if persistence fails the stored file is deleted.
         *
         * @throws com.naturalist.exception.InvariantViolationException for an invalid upload — nothing is stored
         */
        void add(InsectRankName subject, ImageUpload upload, @Nullable NaturalistName naturalist, @Nullable String notes, @Nullable String location);
    }
}
