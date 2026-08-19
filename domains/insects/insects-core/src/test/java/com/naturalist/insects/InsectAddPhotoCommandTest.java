package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class InsectAddPhotoCommandTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(db);
    InsectAddPhotoCommand command = new InsectAddPhotoCommand(context.addPhotoTransaction());
    InsectQuery query = context.insectQuery();

    @Test
    void addPhoto_withNaturalist_insertsImageAndObservation() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var fileName = FileName.of("test-photo-001.jpg");
        var naturalist = NaturalistName.of("test-naturalist");

        command.addPhoto(species, fileName, naturalist, "Found on pipevine", "Garden");

        var images = query.images().forParentName(species).stream().toList();
        var photo = images.stream()
                .filter(img -> img.resourceName().equals(fileName))
                .findFirst();
        assertThat(photo).isPresent();
        assertThat(photo.get().observationId()).isNotNull();

        var observation = query.observations()
                .getByName(photo.get().observationId());
        assertThat(observation).isPresent();
        assertThat(observation.get().observedBy()).isEqualTo(naturalist);
        assertThat(observation.get().subject()).isEqualTo(species);
        assertThat(observation.get().notes()).isEqualTo("Found on pipevine");
        assertThat(observation.get().location()).isEqualTo("Garden");
        assertThat(observation.get().identification()).isNull();
    }

    @Test
    void addPhoto_withoutNaturalist_insertsImageOnly() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var fileName = FileName.of("test-photo-002.jpg");

        command.addPhoto(species, fileName, null, null, null);

        var images = query.images().forParentName(species).stream().toList();
        var photo = images.stream()
                .filter(img -> img.resourceName().equals(fileName))
                .findFirst();
        assertThat(photo).isPresent();
        assertThat(photo.get().observationId()).isNull();
    }

    @Test
    void addPhoto_genusRank_insertsImageAndObservationAtGenus() {
        var genus = TestInsectsIdentifiers.InsectGenus.Battus.name;
        var fileName = FileName.of("test-photo-genus.jpg");
        var naturalist = NaturalistName.of("test-naturalist");

        command.addPhoto(genus, fileName, naturalist, "Unidentified swallowtail", null);

        var images = query.images().forParentName(genus).stream().toList();
        var photo = images.stream()
                .filter(img -> img.resourceName().equals(fileName))
                .findFirst();
        assertThat(photo).isPresent();
        assertThat(photo.get().parentName()).isEqualTo(genus);
        assertThat(photo.get().observationId()).isNotNull();

        var observation = query.observations()
                .getByName(photo.get().observationId());
        assertThat(observation).isPresent();
        assertThat(observation.get().subject()).isEqualTo(genus);
    }

    @Test
    void addPhoto_withNaturalist_blankNotesAndLocation_persistsAsNull() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var fileName = FileName.of("test-photo-003.jpg");
        var naturalist = NaturalistName.of("test-naturalist");

        command.addPhoto(species, fileName, naturalist, "  ", "");

        var images = query.images().forParentName(species).stream().toList();
        var photo = images.stream()
                .filter(img -> img.resourceName().equals(fileName))
                .findFirst();
        assertThat(photo).isPresent();

        var observation = query.observations()
                .getByName(photo.get().observationId());
        assertThat(observation).isPresent();
        assertThat(observation.get().notes()).isNull();
        assertThat(observation.get().location()).isNull();
    }
}
