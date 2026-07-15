package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Adds a photograph to a catalog entry at any Linnaean rank (order, family,
 * genus, or species), optionally recording a {@link FieldObservation} when a
 * naturalist is signed in. Builds a {@link PhotoAddition} aggregate and
 * delegates to an {@link InsectAddPhotoTransaction} so the image and
 * observation are persisted atomically.
 */
public class InsectAddPhotoCommand {

    private final Observer observer = Observer.forClass(InsectAddPhotoCommand.class);
    private final InsectAddPhotoTransaction transaction;

    public InsectAddPhotoCommand(InsectAddPhotoTransaction transaction) {
        this.transaction = transaction;
    }

    public void addPhoto(InsectRankName subject, FileName storedFileName,
                         @Nullable NaturalistName naturalist,
                         @Nullable String notes, @Nullable String location) {
        observer.arguments("addPhoto", i -> i
                        .identifier(subject, "subject")
                        .namedValue(storedFileName, "storedFileName"))
                .throwWhenInvalid();

        FieldObservation observation = null;
        FieldObservationId observationId = null;
        if (naturalist != null) {
            observationId = FieldObservationId.create();
            observation = new FieldObservation(
                    observationId, naturalist, subject, Instant.now(),
                    (notes == null || notes.isBlank()) ? null : notes,
                    (location == null || location.isBlank()) ? null : location,
                    null);
        }

        var image = new InsectImage(
                InsectImageId.create(), subject, Instant.now(),
                storedFileName, observationId);

        transaction.execute(new PhotoAddition(image, observation));
    }
}
