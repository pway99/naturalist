package com.naturalist.plants;

import com.naturalist.data.FileName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.observation.OrganismImage;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PlantImageTest {

    private static final Observer observer = Observer.forClass(PlantImageTest.class);

    @Test
    void fullyPopulatedImageIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedImageIsValid");
        OrganismImage<PlantImageId, PlantObservationId, PlantRankName> image = new OrganismImage<PlantImageId, PlantObservationId, PlantRankName>(
                PlantImageId.create(),
                PlantSpeciesName.of("solanum-lycopersicum"),
                Instant.parse("2026-08-18T15:00:00Z"),
                FileName.of("IMG_9313.HEIC"),
                PlantObservationId.create());

        InvariantObservation result = mo.namedEntity(image, "image");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullObservationIdIsValid() {
        // observationId is nullable-by-design — a shared catalog image has no owning observation.
        MethodObserver mo = observer.forMethod("nullObservationIdIsValid");
        OrganismImage<PlantImageId, PlantObservationId, PlantRankName> image = new OrganismImage<PlantImageId, PlantObservationId, PlantRankName>(
                PlantImageId.create(),
                PlantGenusName.of("trifolium"),
                Instant.parse("2026-08-18T15:00:00Z"),
                FileName.of("IMG_9264.HEIC"),
                null);

        InvariantObservation result = mo.namedEntity(image, "image");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        OrganismImage<PlantImageId, PlantObservationId, PlantRankName> image =
                new OrganismImage<PlantImageId, PlantObservationId, PlantRankName>(null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(image, "image");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".image.id",
                        ".image.parentName",
                        ".image.dateAdded",
                        ".image.resourceName");
    }
}
