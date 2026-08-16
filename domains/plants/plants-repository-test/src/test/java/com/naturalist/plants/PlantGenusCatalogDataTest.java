package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data assertions over the genus catalog, not framework assertions.
 * <p>
 * The load-bearing one is {@link #everyPlantGenusIsCatalogued()}: M2b of the plants
 * consistency plan replaces {@code Plant.taxonomy} with a typed
 * {@code PlantGenusName genusName} and an FK constraint, and that migration is only
 * possible while every genus a plant references has a record. Nothing else fails when
 * a plant is added whose genus is absent — the gap simply reappears, and the
 * backfill has to be redone.
 */
class PlantGenusCatalogDataTest {

    private final NaturalistDatabase db = NaturalistDatabase.create();

    private List<PlantGenus> genera() {
        return db.getNamed(PlantGenusTestEntitySource.class).entityStream().toList();
    }

    private List<Plant> plants() {
        return db.getNamed(PlantTestEntitySource.class).entityStream().toList();
    }

    private List<PlantFamily> families() {
        return db.getNamed(PlantFamilyTestEntitySource.class).entityStream().toList();
    }

    @Test
    void everyPlantGenusIsCatalogued() {
        Set<String> catalogued = genera().stream()
                .map(g -> g.genus().value())
                .collect(Collectors.toSet());

        List<String> referenced = plants().stream()
                .map(p -> p.taxonomy().genus())
                .filter(java.util.Objects::nonNull)
                .map(g -> g.value())
                .distinct()
                .toList();

        assertThat(catalogued)
                .as("every genus a Plant claims must have a PlantGenus record — "
                        + "M2b's typed genusName FK cannot land otherwise")
                .containsAll(referenced);
    }

    @Test
    void everyGenusResolvesToACataloguedFamily() {
        Set<String> familySlugs = families().stream()
                .map(f -> f.name().value())
                .collect(Collectors.toSet());

        assertThat(genera())
                .allSatisfy(g -> assertThat(familySlugs)
                        .as("genus '%s' references family '%s'",
                                g.name().value(), g.familyName().value())
                        .contains(g.familyName().value()));
    }

    @Test
    void eachGenusFamilyEpithetMatchesItsParentRecord() {
        // PlantGenus carries `family` locally so a chain check does not have to
        // resolve the parent. That redundancy is only safe while the two agree.
        Map<String, String> epithetBySlug = families().stream()
                .collect(Collectors.toMap(f -> f.name().value(), f -> f.family().value()));

        assertThat(genera())
                .allSatisfy(g -> assertThat(g.family().value())
                        .as("genus '%s' carries family epithet '%s' but its parent "
                                        + "'%s' says '%s'",
                                g.name().value(), g.family().value(),
                                g.familyName().value(), epithetBySlug.get(g.familyName().value()))
                        .isEqualTo(epithetBySlug.get(g.familyName().value())));
    }

    @Test
    void everyGenusNameIsAValidSlug() {
        assertThat(genera())
                .allSatisfy(g -> assertThat(g.name().isValid())
                        .as("genus slug '%s' must be lower-kebab-case", g.name().value())
                        .isTrue());
    }

    @Test
    void genusSlugIsTheLowercasedGenusEpithet() {
        assertThat(genera())
                .allSatisfy(g -> assertThat(g.name().value())
                        .as("genus record '%s' should be slugged from its epithet '%s'",
                                g.name().value(), g.genus().value())
                        .isEqualTo(g.genus().value().toLowerCase(java.util.Locale.ROOT)));
    }
}
