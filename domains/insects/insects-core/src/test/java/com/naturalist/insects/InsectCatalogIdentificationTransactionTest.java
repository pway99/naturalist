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
import com.naturalist.observation.OrganismObservation;

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
        assertThat(query.orders().getByName(ORDER_NAME)).isPresent();
        assertThat(query.families().getByName(FAMILY_NAME)).isPresent();
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
        var secondObsId = InsectObservationId.create();
        var secondImage = new InsectImage(
                InsectImageId.create(), SPECIES_NAME, Instant.now(),
                FileName.of("IMG_0002.jpg"), secondObsId);
        var secondObs = new OrganismObservation<InsectObservationId, InsectRankName>(
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
        var secondSpeciesName = InsectSpeciesName.of("fabricatus-secundus");
        var secondObsId = InsectObservationId.create();
        var secondSpecies = new InsectSpecies(
                secondSpeciesName, GENUS_NAME,
                TaxonomicSpecies.of("secundus"),
                description(),
                Set.of(CommonName.of("Second Test Fly")),
                null, null, null, null, null, null, null, null, null);
        var secondImage = new InsectImage(
                InsectImageId.create(), secondSpeciesName, Instant.now(),
                FileName.of("IMG_0003.jpg"), secondObsId);
        var secondObs = new OrganismObservation<InsectObservationId, InsectRankName>(
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
        assertThat(query.orders().getByName(ORDER_NAME)).isPresent();
        assertThat(query.families().getByName(FAMILY_NAME)).isPresent();
        assertThat(query.genera().getByName(GENUS_NAME)).isPresent();
    }

    @Test
    void executePersistsFamilyLevelIdentification() {
        var familyName = InsectFamilyName.of("bogusidae");
        var orderName = InsectOrderName.of("neuroptera"); // real seeded order — FK valid
        var obsId = InsectObservationId.create();
        var family = new InsectFamily(
                familyName, orderName,
                TaxonomicFamily.of("Bogusidae"), description(),
                Set.of(CommonName.of("Bogus Lacewings")), null);
        var taxonomy = new TaxonomicClassification(
                TaxonomicOrder.of("Neuroptera"), TaxonomicFamily.of("Bogusidae"),
                null, null);
        var image = new InsectImage(
                InsectImageId.create(), familyName, Instant.now(),
                FileName.of("IMG_0010.jpg"), obsId);
        var observation = new OrganismObservation<InsectObservationId, InsectRankName>(
                obsId, NaturalistName.of("pat"), familyName,
                Instant.now(), null, null, null);
        var id = new CatalogIdentification(
                new IdentifiedRankEntity.Family(family), taxonomy,
                image, observation, List.of(), List.of(), Map.of());

        transaction.execute(id);

        assertThat(query.orders().getByName(orderName)).isPresent();
        var persistedFamily = query.families().getByName(familyName);
        assertThat(persistedFamily).isPresent();
        assertThat(persistedFamily.get().description()).isEqualTo(description());
        assertThat(query.images().forParentName(familyName).stream().toList()).hasSize(1);
        assertThat(query.fieldObservations().getByName(obsId)).isPresent();
    }

    @Test
    void executePersistsFeatures() {
        var featureId = InsectFeatureId.create();
        var feature = InsectFeature.of(featureId, "hovering flight");
        var assignment = InsectFeatureAssignment.of(
                InsectFeatureAssignmentId.create(), featureId,
                SPECIES_NAME, 0);
        var id = new CatalogIdentification(
                new IdentifiedRankEntity.Species(species()), taxonomy(),
                image(), observation(),
                List.of(feature), List.of(assignment), Map.of());

        transaction.execute(id);

        var featureView = query.features().findByRankName(SPECIES_NAME);
        assertThat(featureView).isPresent();
        assertThat(featureView.get().features()).hasSize(1);
        assertThat(featureView.get().features().getFirst().feature().value())
                .isEqualTo("hovering flight");
    }

    @Test
    void executeUsesPreResolvedDescriptionForNewParentRank() {
        var resolvedDescription = new Description(
                "Testoptera are imaginary!",
                "Testoptera have no real wings at all.",
                "Order Testoptera demonstrates remarkable fictitious diversity.",
                "Testoptera is a purely synthetic test order.");
        var parentDescriptions = Map.<InsectRankName, Description>of(
                ORDER_NAME, resolvedDescription);
        var id = new CatalogIdentification(
                new IdentifiedRankEntity.Species(species()), taxonomy(),
                image(), observation(),
                List.of(), List.of(), parentDescriptions);

        transaction.execute(id);

        var order = query.orders().getByName(ORDER_NAME);
        assertThat(order).isPresent();
        assertThat(order.get().description().preschool())
                .isEqualTo("Testoptera are imaginary!");
    }

    // ----- fixtures -----

    // Fictitious taxa — must not collide with any seeded catalog data
    private static final InsectSpeciesName SPECIES_NAME = InsectSpeciesName.of("fabricatus-imaginarius");
    private static final InsectGenusName GENUS_NAME = InsectGenusName.of("fabricatus");
    private static final InsectFamilyName FAMILY_NAME = InsectFamilyName.of("fictitiidae");
    private static final InsectOrderName ORDER_NAME = InsectOrderName.of("testoptera");
    private static final InsectObservationId OBSERVATION_ID = InsectObservationId.create();

    private static CatalogIdentification catalogIdentification() {
        return new CatalogIdentification(
                new IdentifiedRankEntity.Species(species()),
                taxonomy(), image(), observation(),
                List.of(), List.of(), Map.of());
    }

    private static InsectSpecies species() {
        return new InsectSpecies(
                SPECIES_NAME, GENUS_NAME,
                TaxonomicSpecies.of("imaginarius"),
                description(),
                Set.of(CommonName.of("Imaginary Test Fly")),
                null, null, null, null, null, null, null, null, null);
    }

    private static TaxonomicClassification taxonomy() {
        return new TaxonomicClassification(
                TaxonomicOrder.of("Testoptera"),
                TaxonomicFamily.of("Fictitiidae"),
                TaxonomicGenus.of("Fabricatus"),
                TaxonomicSpecies.of("imaginarius"));
    }

    private static InsectImage image() {
        return new InsectImage(
                InsectImageId.create(), SPECIES_NAME, Instant.now(),
                FileName.of("IMG_0001.jpg"), OBSERVATION_ID);
    }

    private static OrganismObservation<InsectObservationId, InsectRankName> observation() {
        return new OrganismObservation<InsectObservationId, InsectRankName>(
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
