package com.naturalist.insects;

import com.naturalist.ddd.Aggregate;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.TaxonomicClassification;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

/**
 * Write-side consistency boundary for insect catalog identification — the
 * aggregate that a {@link com.naturalist.data.Transaction} persists
 * atomically when a naturalist identifies an insect from a photograph.
 *
 * <p>Carries the identified rank entity (polymorphic — species, genus,
 * family, or order), taxonomy, image, observation, structured features,
 * and pre-resolved parent rank descriptions. Cross-entity invariants
 * enforce FK consistency: the image and observation must reference the
 * identified rank.
 */
public record CatalogIdentification(
        IdentifiedRankEntity identifiedEntity,
        TaxonomicClassification taxonomy,
        OrganismImage<InsectImageId, InsectObservationId, InsectRankName> image,
        OrganismObservation<InsectObservationId, InsectRankName> observation,
        List<InsectFeature> newFeatures,
        List<InsectFeatureAssignment> featureAssignments,
        Map<InsectRankName, Description> parentDescriptions
) implements Aggregate {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(identifiedEntity, "identifiedEntity")
                .valueObject(taxonomy, "taxonomy")
                .namedEntity(image, "image")
                .namedEntity(observation, "observation")
                .notNull(newFeatures, "newFeatures")
                .notNull(featureAssignments, "featureAssignments")
                .notNull(parentDescriptions, "parentDescriptions")
                .isTrue(imageParentMatchesIdentifiedRank(),
                        "imageParentMatchesIdentifiedRank")
                .isTrue(observationSubjectMatchesIdentifiedRank(),
                        "observationSubjectMatchesIdentifiedRank")
                .isTrue(imageObservationIdMatchesObservation(),
                        "imageObservationIdMatchesObservation");
    }

    private boolean imageParentMatchesIdentifiedRank() {
        return identifiedEntity == null || image == null
                || image.parentName().equals(identifiedEntity.rankName());
    }

    private boolean observationSubjectMatchesIdentifiedRank() {
        return identifiedEntity == null || observation == null
                || observation.subject().equals(identifiedEntity.rankName());
    }

    private boolean imageObservationIdMatchesObservation() {
        return image == null || observation == null
                || Objects.equals(image.observationId(), observation.id());
    }
}
