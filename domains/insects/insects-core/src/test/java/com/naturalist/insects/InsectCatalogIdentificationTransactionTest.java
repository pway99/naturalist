package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectCatalogIdentificationTransactionTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(db);
    InsectQuery query = context.insectQuery();
    InsectCatalogIdentificationTransaction transaction = context.catalogIdentificationTransaction();

    @Test
    void executePersistsSpeciesAndParentRanksAndImageAndObservation() {
        var id = catalogIdentification();

        transaction.execute(id);

        // Parent ranks created
        assertThat(query.orders().getByName(InsectOrderName.of("diptera"))).isPresent();
        assertThat(query.families().getByName(InsectFamilyName.of("syrphidae"))).isPresent();
        assertThat(query.genera().getByName(GENUS_NAME)).isPresent();

        // Species created
        assertThat(query.species().getByName(SPECIES_NAME)).isPresent();

        // Image persisted
        var images = query.images().forParentName(SPECIES_NAME);
        assertThat(images.stream().toList()).hasSize(1);
        assertThat(images.stream().toList().getFirst().resourceName())
                .isEqualTo(FileName.of("IMG_0001.jpg"));

        // Observation persisted
        assertThat(query.fieldObservations().getByName(OBSERVATION_ID)).isPresent();
        assertThat(query.fieldObservations().getByName(OBSERVATION_ID).get().observedBy())
                .isEqualTo(NaturalistName.of("pat"));
    }

    @Test
    void executeIsIdempotentForExistingSpecies() {
        var first = catalogIdentification();
        transaction.execute(first);

        // Second identification of the same species — different image and observation
        var secondObsId = FieldObservationId.create();
        var secondImage = new InsectImage(
                InsectImageId.create(), SPECIES_NAME, Instant.now(),
                FileName.of("IMG_0002.jpg"), secondObsId);
        var secondObs = new FieldObservation(
                secondObsId, NaturalistName.of("pat"), SPECIES_NAME,
                Instant.now(), "second sighting", null, null);
        var second = new CatalogIdentification(
                new IdentifiedRankEntity.Species(species()),
                taxonomy(), secondImage, secondObs,
                List.of(), List.of(), Map.of());

        transaction.execute(second);

        // Species still exists (not duplicated)
        assertThat(query.species().getByName(SPECIES_NAME)).isPresent();

        // Both images persisted
        var images = query.images().forParentName(SPECIES_NAME);
        assertThat(images.stream().toList()).hasSize(2);

        // Both observations persisted
        assertThat(query.fieldObservations().getByName(OBSERVATION_ID)).isPresent();
        assertThat(query.fieldObservations().getByName(secondObsId)).isPresent();
    }

    @Test
    void executeSkipsExistingParentRanks() {
        // First execution creates all parent ranks
        transaction.execute(catalogIdentification());

        // Second species in same genus — parent ranks already exist
        var secondSpeciesName = InsectSpeciesName.of("eupeodes-volucris");
        var secondObsId = FieldObservationId.create();
        var secondSpecies = new InsectSpecies(
                secondSpeciesName, GENUS_NAME,
                TaxonomicSpecies.of("volucris"),
                description(),
                Set.of(CommonName.of("Bird Hover Fly")),
                null, null, null, null, null, null, null, null, null);
        var secondImage = new InsectImage(
                InsectImageId.create(), secondSpeciesName, Instant.now(),
                FileName.of("IMG_0003.jpg"), secondObsId);
        var secondObs = new FieldObservation(
                secondObsId, NaturalistName.of("pat"), secondSpeciesName,
                Instant.now(), null, null, null);
        var second = new CatalogIdentification(
                new IdentifiedRankEntity.Species(secondSpecies),
                taxonomy(), secondImage, secondObs,
                List.of(), List.of(), Map.of());

        transaction.execute(second);

        // Both species exist
        assertThat(query.species().getByName(SPECIES_NAME)).isPresent();
        assertThat(query.species().getByName(secondSpeciesName)).isPresent();

        // Parent ranks still exist (not duplicated — no PrimaryKeyConstraintException)
        assertThat(query.orders().getByName(InsectOrderName.of("diptera"))).isPresent();
        assertThat(query.families().getByName(InsectFamilyName.of("syrphidae"))).isPresent();
        assertThat(query.genera().getByName(GENUS_NAME)).isPresent();
    }

    // ----- fixtures -----

    private static final InsectSpeciesName SPECIES_NAME = InsectSpeciesName.of("eupeodes-fumipennis");
    private static final InsectGenusName GENUS_NAME = InsectGenusName.of("eupeodes");
    private static final FieldObservationId OBSERVATION_ID = FieldObservationId.create();

    private static CatalogIdentification catalogIdentification() {
        return new CatalogIdentification(
                new IdentifiedRankEntity.Species(species()),
                taxonomy(), image(), observation(),
                List.of(), List.of(), Map.of());
    }

    private static InsectSpecies species() {
        return new InsectSpecies(
                SPECIES_NAME, GENUS_NAME,
                TaxonomicSpecies.of("fumipennis"),
                description(),
                Set.of(CommonName.of("Pacific Hover Fly")),
                null, null, null, null, null, null, null, null, null);
    }

    private static TaxonomicClassification taxonomy() {
        return new TaxonomicClassification(
                TaxonomicOrder.of("Diptera"),
                TaxonomicFamily.of("Syrphidae"),
                TaxonomicGenus.of("Eupeodes"),
                TaxonomicSpecies.of("fumipennis"));
    }

    private static InsectImage image() {
        return new InsectImage(
                InsectImageId.create(), SPECIES_NAME, Instant.now(),
                FileName.of("IMG_0001.jpg"), OBSERVATION_ID);
    }

    private static FieldObservation observation() {
        return new FieldObservation(
                OBSERVATION_ID, NaturalistName.of("pat"), SPECIES_NAME,
                Instant.now(), null, null, null);
    }

    private static Description description() {
        return new Description(
                "A small hover fly that looks like a tiny bee.",
                "Hover flies in the genus Eupeodes are important pollinators and aphid predators.",
                "Eupeodes fumipennis is a Syrphid fly whose larvae are voracious aphid predators.",
                "E. fumipennis is a Palearctic-Nearctic hover fly with larval aphidophagy.");
    }
}
