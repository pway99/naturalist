package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.data.FileName;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.InsectEntityCollections.ImageGallery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageGalleryTest {

    private static final InsectSpeciesName SPECIES_A = InsectSpeciesName.of("species-a");
    private static final InsectSpeciesName SPECIES_B = InsectSpeciesName.of("species-b");
    private static final InsectGenusName GENUS_C = InsectGenusName.of("genus-c");
    private static final InsectOrderName ORDER_X = InsectOrderName.of("order-x");

    private static OrganismImage<InsectImageId, InsectObservationId, InsectRankName> image(InsectRankName parent, String filename) {
        return new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                InsectImageId.create(),
                parent,
                Instant.now(),
                FileName.of(filename),
                null);
    }

    @Test
    void ofGroupsByParentName() {
        var imgA1 = image(SPECIES_A, "a1.heic");
        var imgA2 = image(SPECIES_A, "a2.heic");
        var imgB1 = image(SPECIES_B, "b1.heic");

        ImageGallery gallery = ImageGallery.of(List.of(imgA1, imgA2, imgB1));

        assertThat(gallery.forEntity(SPECIES_A).stream().toList())
                .containsExactlyInAnyOrder(imgA1, imgA2);
        assertThat(gallery.forEntity(SPECIES_B).stream().toList())
                .containsExactly(imgB1);
        assertThat(gallery.size()).isEqualTo(3);
    }

    @Test
    void groupedUsesProvidedMapping() {
        var imgA1 = image(SPECIES_A, "a1.heic");
        var imgB1 = image(SPECIES_B, "b1.heic");

        ImageGallery gallery = ImageGallery.grouped(
                Map.of(ORDER_X, List.of(imgA1, imgB1)));

        assertThat(gallery.forEntity(ORDER_X).stream().toList())
                .containsExactlyInAnyOrder(imgA1, imgB1);
        assertThat(gallery.forEntity(SPECIES_A).isEmpty()).isTrue();
        assertThat(gallery.size()).isEqualTo(2);
    }

    @Test
    void forEntityReturnsEmptyForUnknownKey() {
        ImageGallery gallery = ImageGallery.of(List.of(image(SPECIES_A, "a.heic")));

        ImageCollection result = gallery.forEntity(SPECIES_B);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void forEntityWithNullKeyReturnsEmptyWithoutThrowing() {
        ImageGallery gallery = ImageGallery.of(List.of(image(SPECIES_A, "a.heic")));

        ImageCollection result = gallery.forEntity(null);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void emptyGallery() {
        ImageGallery gallery = ImageGallery.empty();

        assertThat(gallery.isEmpty()).isTrue();
        assertThat(gallery.size()).isEqualTo(0);
        assertThat(gallery.forEntity(SPECIES_A).isEmpty()).isTrue();
    }

    @Test
    void hasKeyReflectsContent() {
        ImageGallery gallery = ImageGallery.of(List.of(image(GENUS_C, "c.heic")));

        assertThat(gallery.hasKey(GENUS_C)).isTrue();
        assertThat(gallery.hasKey(SPECIES_A)).isFalse();
        assertThat(gallery.hasKey(null)).isFalse();
    }

    @Test
    void streamReturnsAllElementsFlat() {
        var img1 = image(SPECIES_A, "a.heic");
        var img2 = image(SPECIES_B, "b.heic");

        ImageGallery gallery = ImageGallery.of(List.of(img1, img2));

        assertThat(gallery.stream().toList())
                .containsExactlyInAnyOrder(img1, img2);
    }
}
