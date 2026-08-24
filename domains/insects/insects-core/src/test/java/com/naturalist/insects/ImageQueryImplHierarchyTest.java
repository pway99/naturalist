package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ImageQueryImplHierarchyTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(nte);
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

    @Test
    void forRankHierarchies_groupsEachRootSubtreeMatchingForRankHierarchy() {
        // Two non-ancestral roots at different levels: a genus under Lepidoptera and an unrelated
        // order. Each root's bucket must equal what forRankHierarchy returns for it alone.
        InsectRankName battus = TestInsectsIdentifiers.InsectGenus.Battus.name;
        InsectRankName hymenoptera = TestInsectsIdentifiers.InsectOrder.Hymenoptera.name;

        var gallery = query.images().forRankHierarchies(java.util.Set.of(battus, hymenoptera));

        assertThat(gallery.keys()).containsExactlyInAnyOrder(battus, hymenoptera);
        assertThat(gallery.forEntity(battus).stream().toList())
                .hasSameElementsAs(query.images().forRankHierarchy(battus).stream().toList());
        assertThat(gallery.forEntity(hymenoptera).stream().toList())
                .hasSameElementsAs(query.images().forRankHierarchy(hymenoptera).stream().toList());
    }

    @Test
    void forRankHierarchies_unknownRoot_isPresentButEmpty() {
        InsectRankName unknown = InsectGenusName.of("nonexistent-genus");
        var gallery = query.images().forRankHierarchies(java.util.Set.of(unknown));

        assertThat(gallery.keys()).containsExactly(unknown);
        assertThat(gallery.forEntity(unknown).stream().toList()).isEmpty();
    }
}
