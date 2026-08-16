package com.naturalist.garden;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.plants.PlantGenusName;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.PlantRankName;
import com.naturalist.taxonomy.LinealRank;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link PlantingRepository}, plus the three reverse lookups: what is in
 * this bed, what is in this row, and where have we grown this taxon.
 */
interface PlantingEntityRepositoryTest extends EntityRepositoryTest<PlantingId, Planting> {

    @Override
    PlantingRepository repository();

    @Override
    default TestEntitySource<PlantingId, Planting> source() {
        return db.getNamed(PlantingTestEntitySource.class);
    }

    @Override
    default PlantingId notFoundName() {
        return TestGardenIdentifiers.Plantings.notFound;
    }

    @Override
    default List<PlantingId> knownEntityNames() {
        return List.of(
                TestGardenIdentifiers.Plantings.amishPasteBackyard,
                TestGardenIdentifiers.Plantings.genoveseBasilBox1);
    }

    @Override
    default Planting newEntity() {
        return sample(PlantingId.create());
    }

    @Override
    default Planting ghostEntity() {
        return sample(PlantingId.create());
    }

    /**
     * Every mutable field changed, including the two that record a planting ending: a removed date
     * where there was none, and the variety becoming known where it was not.
     */
    @Override
    default Planting modifiedEntity(Planting original) {
        return new Planting(
                original.id(),
                TestGardenIdentifiers.Plants.basil,
                TestGardenIdentifiers.Cultivars.sungoldCherry,
                ZoneName.of(RandomValue.string()),
                SubZoneName.of(RandomValue.string()),
                7,
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 9, 1),
                RandomValue.string());
    }

    @Test
    default void getByZoneName_nullArgument() {
        assertThatThrownBy(() -> repository().getByZoneName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("zoneName");
    }

    @Test
    default void getByZoneName_unknown_returnsEmpty() {
        assertThat(repository().getByZoneName(TestGardenIdentifiers.Zones.notFound)).isEmpty();
    }

    /**
     * A bed query returns everything in the bed regardless of species — the back yard holds
     * tomatoes and an eggplant, and collapsing it to one plant would be a lie about the bed.
     */
    @Test
    default void getByZoneName_known_returnsEverySpeciesInThatBed() {
        List<Planting> result = repository().getByZoneName(TestGardenIdentifiers.Zones.backyard);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(Planting::plantName)
                .contains(TestGardenIdentifiers.Plants.tomato, TestGardenIdentifiers.Plants.eggplant);
    }

    @Test
    default void getBySubZoneName_nullArgument() {
        assertThatThrownBy(() -> repository().getBySubZoneName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("subZoneName");
    }

    @Test
    default void getBySubZoneName_unknown_returnsEmpty() {
        assertThat(repository().getBySubZoneName(SubZoneName.of("unobtainium-row"))).isEmpty();
    }

    /**
     * One row, two species. The south row carries Amish Paste tomatoes and the Black Beauty
     * eggplant, which is why a sub-zone claims nothing about what is planted in it.
     */
    @Test
    default void getBySubZoneName_known_returnsAMixedRowIntact() {
        List<Planting> result = repository().getBySubZoneName(SubZoneName.of("backyard-south"));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Planting::plantName)
                .containsExactlyInAnyOrder(TestGardenIdentifiers.Plants.tomato,
                        TestGardenIdentifiers.Plants.eggplant);
    }

    @Test
    default void getByPlantName_nullArgument() {
        assertThatThrownBy(() -> repository().getByPlantName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("plantName");
    }

    @Test
    default void getByPlantName_unknown_returnsEmpty() {
        assertThat(repository().getByPlantName(TestGardenIdentifiers.Plants.notFound)).isEmpty();
    }

    /** Four varieties of one species, across two beds. */
    @Test
    default void getByPlantName_known_returnsEveryVarietyOfThatSpecies() {
        List<Planting> result = repository().getByPlantName(TestGardenIdentifiers.Plants.tomato);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(Planting::cultivarName)
                .containsExactlyInAnyOrder(
                        TestGardenIdentifiers.Cultivars.amishPaste,
                        TestGardenIdentifiers.Cultivars.italianPearNicks,
                        TestGardenIdentifiers.Cultivars.sanMarzanoF2,
                        TestGardenIdentifiers.Cultivars.sungoldCherry);
    }

    /** Sown from a mixed packet: the species is known and the variety never was. */
    @Test
    default void getByName_aPlantingWithNoRecordedVariety() {
        Planting radish = repository().getByName(TestGardenIdentifiers.Plantings.radishBox1)
                .orElseThrow();

        assertThat(radish.plantName()).isEqualTo(TestGardenIdentifiers.Plants.radish);
        assertThat(radish.isVarietyKnown()).isFalse();
    }

    /**
     * A tray of unlabelled salvia starts: the gardener knows the genus and no more. Before
     * {@code plantName} was widened to {@link PlantRankName} this was unrepresentable — the
     * field took a species name, so a genus-level planting had to be either guessed up to a
     * species or left unrecorded.
     */
    @Test
    default void insert_aPlantingIdentifiedOnlyToGenus() {
        PlantGenusName salvia = PlantGenusName.of("salvia");
        Planting genusRank = new Planting(
                PlantingId.create(), salvia, null,
                ZoneName.of(RandomValue.string()), null, 6,
                LocalDate.of(2026, 5, 1), null, null);

        repository().insert(genusRank);

        Planting stored = repository().getByName(genusRank.id()).orElseThrow();
        assertThat(stored.plantName()).isEqualTo(salvia);
        assertThat(stored.plantName().rank()).isEqualTo(LinealRank.GENUS);
        assertThat(stored.isVarietyKnown()).isFalse();
    }

    /** Rank names of different rank never collide, even holding the same slug. */
    @Test
    default void getByPlantName_doesNotMatchAcrossRanks() {
        assertThat(repository().getByPlantName(PlantGenusName.of(
                TestGardenIdentifiers.Plants.tomato.value()))).isEmpty();
    }

    private static Planting sample(PlantingId id) {
        return new Planting(
                id,
                PlantSpeciesName.of(RandomValue.string()),
                null,
                ZoneName.of(RandomValue.string()),
                null,
                12,
                LocalDate.of(2026, 4, 6),
                null,
                null);
    }
}
