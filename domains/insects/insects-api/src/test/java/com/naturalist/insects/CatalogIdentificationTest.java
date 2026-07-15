package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.FileName;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogIdentificationTest {

    private static final Observer observer = Observer.forClass(CatalogIdentificationTest.class);

    @Test
    void validCatalogIdentificationHasNoViolations() {
        MethodObserver mo = observer.forMethod("validCatalogIdentificationHasNoViolations");
        CatalogIdentification id = validCatalogIdentification();

        InvariantObservation result = mo.observable(id, "catalogIdentification");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullSpeciesReportsViolation() {
        MethodObserver mo = observer.forMethod("nullSpeciesReportsViolation");
        CatalogIdentification id = new CatalogIdentification(
                null, taxonomy(), image(), observation());

        InvariantObservation result = mo.observable(id, "catalogIdentification");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".catalogIdentification.species");
    }

    @Test
    void nullTaxonomyReportsViolation() {
        MethodObserver mo = observer.forMethod("nullTaxonomyReportsViolation");
        CatalogIdentification id = new CatalogIdentification(
                species(), null, image(), observation());

        InvariantObservation result = mo.observable(id, "catalogIdentification");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".catalogIdentification.taxonomy");
    }

    @Test
    void nullImageReportsViolation() {
        MethodObserver mo = observer.forMethod("nullImageReportsViolation");
        CatalogIdentification id = new CatalogIdentification(
                species(), taxonomy(), null, observation());

        InvariantObservation result = mo.observable(id, "catalogIdentification");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".catalogIdentification.image");
    }

    @Test
    void nullObservationReportsViolation() {
        MethodObserver mo = observer.forMethod("nullObservationReportsViolation");
        CatalogIdentification id = new CatalogIdentification(
                species(), taxonomy(), image(), null);

        InvariantObservation result = mo.observable(id, "catalogIdentification");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".catalogIdentification.observation");
    }

    @Test
    void mismatchedImageParentReportsViolation() {
        MethodObserver mo = observer.forMethod("mismatchedImageParentReportsViolation");
        var wrongParent = new InsectImage(
                InsectImageId.create(),
                InsectSpeciesName.of("wrong-species"),
                Instant.now(),
                FileName.of("IMG_0001.jpg"),
                OBSERVATION_ID);
        CatalogIdentification id = new CatalogIdentification(
                species(), taxonomy(), wrongParent, observation());

        InvariantObservation result = mo.observable(id, "catalogIdentification");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".catalogIdentification.imageParentMatchesSpecies");
    }

    @Test
    void mismatchedObservationSubjectReportsViolation() {
        MethodObserver mo = observer.forMethod("mismatchedObservationSubjectReportsViolation");
        var wrongSubject = new FieldObservation(
                OBSERVATION_ID,
                NaturalistName.of("pat"),
                InsectSpeciesName.of("wrong-species"),
                Instant.now(),
                null, null, null);
        CatalogIdentification id = new CatalogIdentification(
                species(), taxonomy(), image(), wrongSubject);

        InvariantObservation result = mo.observable(id, "catalogIdentification");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".catalogIdentification.observationSubjectMatchesSpecies");
    }

    @Test
    void mismatchedImageObservationIdReportsViolation() {
        MethodObserver mo = observer.forMethod("mismatchedImageObservationIdReportsViolation");
        var differentObservationId = FieldObservationId.create();
        var imageWithWrongObsId = new InsectImage(
                InsectImageId.create(),
                SPECIES_NAME,
                Instant.now(),
                FileName.of("IMG_0001.jpg"),
                differentObservationId);
        CatalogIdentification id = new CatalogIdentification(
                species(), taxonomy(), imageWithWrongObsId, observation());

        InvariantObservation result = mo.observable(id, "catalogIdentification");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".catalogIdentification.imageObservationIdMatchesObservation");
    }

    // ----- fixtures -----

    private static final InsectSpeciesName SPECIES_NAME = InsectSpeciesName.of("vanessa-cardui");
    private static final InsectGenusName GENUS_NAME = InsectGenusName.of("vanessa");
    private static final FieldObservationId OBSERVATION_ID = FieldObservationId.create();

    private static CatalogIdentification validCatalogIdentification() {
        return new CatalogIdentification(species(), taxonomy(), image(), observation());
    }

    private static InsectSpecies species() {
        return new InsectSpecies(
                SPECIES_NAME, GENUS_NAME,
                TaxonomicSpecies.of("cardui"),
                description(),
                Set.of(CommonName.of("Painted Lady")),
                null, null, null, null, null, null, null, null, null);
    }

    private static TaxonomicClassification taxonomy() {
        return new TaxonomicClassification(
                TaxonomicOrder.of("Lepidoptera"),
                TaxonomicFamily.of("Nymphalidae"),
                TaxonomicGenus.of("Vanessa"),
                TaxonomicSpecies.of("cardui"));
    }

    private static InsectImage image() {
        return new InsectImage(
                InsectImageId.create(),
                SPECIES_NAME,
                Instant.now(),
                FileName.of("IMG_0001.jpg"),
                OBSERVATION_ID);
    }

    private static FieldObservation observation() {
        return new FieldObservation(
                OBSERVATION_ID,
                NaturalistName.of("pat"),
                SPECIES_NAME,
                Instant.now(),
                null, null, null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
