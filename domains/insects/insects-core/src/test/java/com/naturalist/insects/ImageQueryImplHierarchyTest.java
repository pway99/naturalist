package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ImageQueryImplHierarchyTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(db);
    InsectQuery query = context.insectQuery();

    @Test
    void forRankHierarchy_species_returnsSpeciesImages() {
        var speciesName = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var directImages = query.images().forParentName(speciesName);
        var hierarchyImages = query.images().forRankHierarchy(speciesName);

        // For a species, hierarchy == direct
        assertThat(hierarchyImages.stream().toList())
                .hasSameSizeAs(directImages.stream().toList());
    }

    @Test
    void forRankHierarchy_genus_includesSpeciesImages() {
        // Battus genus — contains at least BattusPhilenor species
        var genusName = TestInsectsIdentifiers.InsectGenus.Battus.name;
        var hierarchyImages = query.images().forRankHierarchy(genusName);

        // Should include genus-level images + all member species images
        var directGenusImages = query.images().forParentName(genusName);
        var speciesInGenus = query.species().forGenusName(genusName);
        int expectedCount = directGenusImages.stream().toList().size();
        for (var species : speciesInGenus.stream().toList()) {
            expectedCount += query.images().forParentName(species.name()).stream().toList().size();
        }

        assertThat(hierarchyImages.stream().toList()).hasSize(expectedCount);
    }

    @Test
    void forRankHierarchy_order_includesAllDescendantImages() {
        var orderName = TestInsectsIdentifiers.InsectOrder.Hymenoptera.name;
        var hierarchyImages = query.images().forRankHierarchy(orderName);

        // Should include images from the order, its families, genera, and species
        var directOrderImages = query.images().forParentName(orderName);
        assertThat(hierarchyImages.stream().toList().size())
                .isGreaterThanOrEqualTo(directOrderImages.stream().toList().size());
    }

    @Test
    void forRankHierarchy_subspecies_returnsEmpty() {
        var subspeciesName = InsectSubspeciesName.of("battus-philenor-hirsuta");
        var result = query.images().forRankHierarchy(subspeciesName);
        assertThat(result.stream().toList()).isEmpty();
    }
}
