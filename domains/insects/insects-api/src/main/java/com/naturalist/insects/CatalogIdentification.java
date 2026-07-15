package com.naturalist.insects;

import com.naturalist.ddd.Aggregate;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.TaxonomicClassification;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Write-side consistency boundary for insect catalog identification — the
 * aggregate that an {@link com.naturalist.data.Transaction} persists
 * atomically when a naturalist identifies an insect from a photograph.
 *
 * <p>Carries the four entities the transaction must persist (species,
 * image, observation) plus the full {@link TaxonomicClassification} needed
 * to create any missing parent ranks (order, family, genus). Cross-entity
 * invariants enforce FK consistency: the image and observation must
 * reference the same species, and the image must link to the observation.
 *
 * <p>Read/write symmetry: {@link Insect} is the read-side composition
 * (ReadModel); this record is the write-side boundary (Aggregate).
 */
public record CatalogIdentification(
        InsectSpecies species,
        TaxonomicClassification taxonomy,
        InsectImage image,
        FieldObservation observation
) implements Aggregate {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(species, "species")
                .valueObject(taxonomy, "taxonomy")
                .namedEntity(image, "image")
                .namedEntity(observation, "observation")
                .isTrue(image.parentName().equals(species.name()),
                        "imageParentMatchesSpecies")
                .isTrue(observation.subject().equals(species.name()),
                        "observationSubjectMatchesSpecies")
                .isTrue(Objects.equals(image.observationId(), observation.id()),
                        "imageObservationIdMatchesObservation");
    }
}
