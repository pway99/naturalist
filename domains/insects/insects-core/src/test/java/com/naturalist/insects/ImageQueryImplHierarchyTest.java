package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ImageQueryImplHierarchyTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired
    InsectQuery query;

    @Test
    void forRankHierarchy_species_returnsSpeciesImages() {
        InsectSpeciesName speciesName = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        InsectEntityCollections.ImageCollection directImages = query.images().forParentName(speciesName);
        InsectEntityCollections.ImageCollection hierarchyImages = query.images().forRankHierarchy(speciesName);

        // For a species, hierarchy == direct
        assertThat(hierarchyImages.stream().toList())
                .hasSameSizeAs(directImages.stream().toList());
    }

    @Test
    void forRankHierarchy_genus_includesSpeciesImages() {
        // Battus genus — contains at least BattusPhilenor species
        InsectGenusName genusName = TestInsectsIdentifiers.InsectGenus.Battus.name;
        InsectEntityCollections.ImageCollection hierarchyImages = query.images().forRankHierarchy(genusName);

        // Should include genus-level images + all member species images
        InsectEntityCollections.ImageCollection directGenusImages = query.images().forParentName(genusName);
        InsectEntityCollections.SpeciesCollection speciesInGenus = query.species().forGenusName(genusName);
        int expectedCount = directGenusImages.stream().toList().size();
        for (InsectSpeciesInfo species : speciesInGenus.stream().toList()) {
            expectedCount += query.images().forParentName(species.name()).stream().toList().size();
        }

        assertThat(hierarchyImages.stream().toList()).hasSize(expectedCount);
    }

    @Test
    void forRankHierarchy_order_includesAllDescendantImages() {
        InsectOrderName orderName = TestInsectsIdentifiers.InsectOrder.Hymenoptera.name;
        InsectEntityCollections.ImageCollection hierarchyImages = query.images().forRankHierarchy(orderName);

        // Should include images from the order, its families, genera, and species
        InsectEntityCollections.ImageCollection directOrderImages = query.images().forParentName(orderName);
        assertThat(hierarchyImages.stream().toList().size())
                .isGreaterThanOrEqualTo(directOrderImages.stream().toList().size());
    }

    @Test
    void forRankHierarchy_subspecies_returnsEmpty() {
        InsectSubspeciesName subspeciesName = InsectSubspeciesName.of("battus-philenor-hirsuta");
        InsectEntityCollections.ImageCollection result = query.images().forRankHierarchy(subspeciesName);
        assertThat(result.stream().toList()).isEmpty();
    }

    @Test
    void forRankHierarchies_groupsEachRootSubtreeMatchingForRankHierarchy() {
        // Two non-ancestral roots at different levels: a genus under Lepidoptera and an unrelated
        // order. Each root's bucket must equal what forRankHierarchy returns for it alone.
        InsectRankName battus = TestInsectsIdentifiers.InsectGenus.Battus.name;
        InsectRankName hymenoptera = TestInsectsIdentifiers.InsectOrder.Hymenoptera.name;

        InsectEntityCollections.ImageGallery gallery = query.images().forRankHierarchies(java.util.Set.of(battus, hymenoptera));

        assertThat(gallery.keys()).containsExactlyInAnyOrder(battus, hymenoptera);
        assertThat(gallery.forEntity(battus).stream().toList())
                .hasSameElementsAs(query.images().forRankHierarchy(battus).stream().toList());
        assertThat(gallery.forEntity(hymenoptera).stream().toList())
                .hasSameElementsAs(query.images().forRankHierarchy(hymenoptera).stream().toList());
    }

    @Test
    void forRankHierarchies_unknownRoot_isPresentButEmpty() {
        InsectRankName unknown = InsectGenusName.of("nonexistent-genus");
        InsectEntityCollections.ImageGallery gallery = query.images().forRankHierarchies(java.util.Set.of(unknown));

        assertThat(gallery.keys()).containsExactly(unknown);
        assertThat(gallery.forEntity(unknown).stream().toList()).isEmpty();
    }
}
