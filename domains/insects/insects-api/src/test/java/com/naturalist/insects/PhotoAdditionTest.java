package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoAdditionTest {

    private static final Observer observer = Observer.forClass(PhotoAdditionTest.class);
    private static final InsectSpeciesName SPECIES_NAME = InsectSpeciesName.of("vanessa-cardui");
    private static final FieldObservationId OBSERVATION_ID = FieldObservationId.create();

    @Test
    void validWithObservation_hasNoViolations() {
        MethodObserver mo = observer.forMethod("validWithObservation_hasNoViolations");
        PhotoAddition addition = new PhotoAddition(image(OBSERVATION_ID), observation());

        InvariantObservation result = mo.observable(addition, "photoAddition");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void validWithoutObservation_hasNoViolations() {
        MethodObserver mo = observer.forMethod("validWithoutObservation_hasNoViolations");
        PhotoAddition addition = new PhotoAddition(image(null), null);

        InvariantObservation result = mo.observable(addition, "photoAddition");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullImageReportsViolation() {
        MethodObserver mo = observer.forMethod("nullImageReportsViolation");
        PhotoAddition addition = new PhotoAddition(null, observation());

        InvariantObservation result = mo.observable(addition, "photoAddition");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".photoAddition.image");
    }

    @Test
    void mismatchedObservationIdReportsViolation() {
        MethodObserver mo = observer.forMethod("mismatchedObservationIdReportsViolation");
        var differentObsId = FieldObservationId.create();
        PhotoAddition addition = new PhotoAddition(image(differentObsId), observation());

        InvariantObservation result = mo.observable(addition, "photoAddition");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".photoAddition.imageObservationIdMatchesObservation");
    }

    @Test
    void imageWithObservationIdButNoObservationReportsViolation() {
        MethodObserver mo = observer.forMethod("imageWithObservationIdButNoObservationReportsViolation");
        PhotoAddition addition = new PhotoAddition(image(OBSERVATION_ID), null);

        InvariantObservation result = mo.observable(addition, "photoAddition");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".photoAddition.imageObservationIdMatchesObservation");
    }

    @Test
    void mismatchedSubjectReportsViolation() {
        MethodObserver mo = observer.forMethod("mismatchedSubjectReportsViolation");
        var wrongSubject = new FieldObservation(
                OBSERVATION_ID,
                NaturalistName.of("pat"),
                InsectSpeciesName.of("wrong-species"),
                Instant.now(),
                null, null, null);
        PhotoAddition addition = new PhotoAddition(image(OBSERVATION_ID), wrongSubject);

        InvariantObservation result = mo.observable(addition, "photoAddition");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".photoAddition.observationSubjectMatchesImageParent");
    }

    @Test
    void genusRankImage_validWithObservation() {
        MethodObserver mo = observer.forMethod("genusRankImage_validWithObservation");
        var genusName = InsectGenusName.of("vanessa");
        var obsId = FieldObservationId.create();
        var genusImage = new InsectImage(
                InsectImageId.create(), genusName, Instant.now(),
                FileName.of("IMG_GENUS.jpg"), obsId);
        var genusObservation = new FieldObservation(
                obsId, NaturalistName.of("pat"), genusName,
                Instant.now(), null, null, null);
        PhotoAddition addition = new PhotoAddition(genusImage, genusObservation);

        InvariantObservation result = mo.observable(addition, "photoAddition");

        assertThat(result.violations()).isEmpty();
    }

    // ----- fixtures -----

    private static InsectImage image(FieldObservationId observationId) {
        return new InsectImage(
                InsectImageId.create(), SPECIES_NAME, Instant.now(),
                FileName.of("IMG_0001.jpg"), observationId);
    }

    private static FieldObservation observation() {
        return new FieldObservation(
                OBSERVATION_ID, NaturalistName.of("pat"), SPECIES_NAME,
                Instant.now(), null, null, null);
    }
}
