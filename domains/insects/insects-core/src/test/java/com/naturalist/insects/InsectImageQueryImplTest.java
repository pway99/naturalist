package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectImageQueryImplTest
        implements EntityQueryContractTest<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, ImageCollection> {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectImageRepositoryMock repository = new InsectImageRepositoryMock(nte);
    InsectFamilyRepositoryMock familyRepository = new InsectFamilyRepositoryMock(nte);
    InsectGenusRepositoryMock genusRepository = new InsectGenusRepositoryMock(nte);
    InsectSpeciesRepositoryMock speciesRepository = new InsectSpeciesRepositoryMock(nte);
    InsectQuery.FamilyQuery familyQuery = new InsectFamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new InsectGenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new InsectSpeciesQueryImpl(speciesRepository, genusQuery);
    InsectQuery.ImageQuery query = new InsectImageQueryImpl(
            repository, speciesQuery, genusQuery, familyQuery);

    @Override
    public EntityQuery<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, ImageCollection> query() {
        return query;
    }

    @Override
    public InsectImageId notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.imageId;
    }

    @Override
    public List<InsectImageId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9047.id,
                TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9048.id);
    }

    @Test
    void forParentName_returnsAllImagesForThatGenus() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(collection.size()).isGreaterThanOrEqualTo(2);
        assertThat(collection.stream())
                .allMatch(image -> image.parentName()
                        .equals(TestInsectsIdentifiers.InsectGenus.Empoasca.name));
        assertThat(collection.stream().map(OrganismImage::id))
                .contains(
                        TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9047.id,
                        TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9048.id);
    }

    @Test
    void forParentName_speciesWithNoImages_returnsEmpty() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectSpecies.HippodamiaConvergens.name);

        assertThat(collection).isNotNull();
        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forParentName_unknownSpecies_returnsEmpty() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forParentName_rejectsNull() {
        assertThatThrownBy(() -> query.forParentName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("parentName");
    }

    @Test
    void forParentName_acceptsSpeciesName() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(collection.size()).isGreaterThanOrEqualTo(1);
        assertThat(collection.stream())
                .allMatch(image -> image.parentName()
                        .equals(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
    }

    @Test
    void forParentName_acceptsFamilyName() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectFamily.Cicadellidae.name);

        assertThat(collection).isNotNull();
        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forParentNames_rejectsNull() {
        assertThatThrownBy(() -> query.forParentNames(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("parentNames");
    }

    @Test
    void forParentNames_returnsImagesAcrossTheGivenParents() {
        ImageCollection collection = query.forParentNames(Set.of(
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));

        assertThat(collection.stream().map(OrganismImage::id))
                .contains(
                        TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9047.id,
                        TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9048.id);
        assertThat(collection.stream())
                .allSatisfy(image -> assertThat(image.parentName()).isIn(
                        TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                        TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
    }
}
