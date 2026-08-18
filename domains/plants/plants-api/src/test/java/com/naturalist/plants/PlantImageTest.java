package com.naturalist.plants;

import com.naturalist.data.FileName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PlantImageTest {

    private static final Observer observer = Observer.forClass(PlantImageTest.class);

    @Test
    void fullyPopulatedImageIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedImageIsValid");
        PlantImage image = new PlantImage(
                PlantImageId.create(),
                PlantSpeciesName.of("solanum-lycopersicum"),
                Instant.parse("2026-08-18T15:00:00Z"),
                FileName.of("IMG_9313.HEIC"),
                FieldObservationId.create());

        InvariantObservation result = mo.namedEntity(image, "image");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullObservationIdIsValid() {
        // observationId is nullable-by-design — a shared catalog image has no owning observation.
        MethodObserver mo = observer.forMethod("nullObservationIdIsValid");
        PlantImage image = new PlantImage(
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
        PlantImage image = new PlantImage(null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(image, "image");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".image.id",
                        ".image.parentName",
                        ".image.dateAdded",
                        ".image.resourceName");
    }
}
