package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Adds a photograph to a catalog entry at any Linnaean rank (order, family,
 * genus, or species), optionally recording a {@link OrganismObservation} when a
 * naturalist is signed in. Stores the upload through the {@link ImageStore}, then
 * builds a {@link PhotoAddition} aggregate and delegates to an
 * {@link InsectAddPhotoTransaction} so the image and observation are persisted
 * atomically; if persistence fails the stored file is deleted.
 */
@DomainService
class InsectPhotoCommandImpl implements InsectCommand.PhotoCommand {

    /** The image-store space holding insect photos. */
    public static final String SPACE = "insects";

    private final Observer observer = Observer.forClass(InsectPhotoCommandImpl.class);
    private final ImageStore imageStore;
    private final InsectAddPhotoTransaction transaction;

    InsectPhotoCommandImpl(ImageStore imageStore, InsectAddPhotoTransaction transaction) {
        observer.arguments("constructor", i -> i
                        .notNull(imageStore, "imageStore")
                        .notNull(transaction, "transaction"))
                .throwWhenInvalid();
        this.imageStore = imageStore;
        this.transaction = transaction;
    }

    @Override
    public void add(InsectRankName subject, ImageUpload upload,
                    @Nullable NaturalistName naturalist,
                    @Nullable String notes, @Nullable String location) {
        observer.arguments("add", i -> i
                        .identifier(subject, "subject")
                        .valueObject(upload, "upload")
                        .whenNotNull(naturalist, c -> c.entityName(naturalist, "naturalist")))
                .throwWhenInvalid();

        FileName stored = imageStore.store(SPACE, upload);

        OrganismObservation<InsectObservationId, InsectRankName> observation = null;
        InsectObservationId observationId = null;
        if (naturalist != null) {
            observationId = InsectObservationId.create();
            observation = new OrganismObservation<InsectObservationId, InsectRankName>(
                    observationId, naturalist, subject, Instant.now(),
                    (notes == null || notes.isBlank()) ? null : notes,
                    (location == null || location.isBlank()) ? null : location,
                    null,
                    null);
        }

        var image = new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                InsectImageId.create(), subject, Instant.now(), stored, observationId);

        try {
            transaction.execute(new PhotoAddition(image, observation));
        } catch (RuntimeException e) {
            try {
                imageStore.delete(SPACE, stored);
            } catch (RuntimeException deleteFailure) {
                e.addSuppressed(deleteFailure);
            }
            throw e;
        }
    }
}
