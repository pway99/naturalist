package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.ImageUpload;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectViewQueryImplTest {
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};

    private static final NaturalistName FLORA = NaturalistName.of("flora-mendez");

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(nte);

    InsectViewQuery views = context.insectViewQuery();

    InsectRankName species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;

    @Test
    void rankPage_noViewer_carriesTheSameInsectAsGetByName() {
        InsectViews.RankPage page = views.rankPage(species, null).orElseThrow();

        assertThat(page.insect()).isEqualTo(context.insectQuery().getByName(species).orElseThrow());
        assertThat(page.viewer()).isEqualTo(InsectViews.Viewer.anonymous());
    }

    @Test
    void rankPage_viewerWithoutObservations() {
        InsectViews.RankPage page = views.rankPage(species, FLORA).orElseThrow();

        assertThat(page.viewer().collected()).isFalse();
        assertThat(page.viewer().ownImages().stream().toList()).isEmpty();
        assertThat(page.viewer().ownObservationByImage()).isEmpty();
        assertThat(page.insect().observations().stream().toList())
                .isEqualTo(context.insectQuery().getByName(species).orElseThrow().observations().stream().toList());
    }

    @Test
    void rankPage_viewerWithPhotoObservation() {
        context.insectCommand().photos().add(species, ImageUpload.of(JPEG), FLORA, "on the pipevine", null);

        InsectViews.RankPage page = views.rankPage(species, FLORA).orElseThrow();

        InsectViews.Viewer viewer = page.viewer();
        assertThat(viewer.collected()).isTrue();
        assertThat(viewer.ownImages().stream().toList()).hasSize(1);
        var image = viewer.ownImages().stream().findFirst().orElseThrow();
        assertThat(viewer.ownObservationByImage()).containsOnlyKeys(image.id());
        assertThat(viewer.ownObservationByImage().get(image.id()).observedBy()).isEqualTo(FLORA);
        assertThat(viewer.ownObservationByImage().get(image.id()).id()).isEqualTo(image.observationId());
        assertThat(page.insect().observations().stream().toList()).contains(image);
        assertThatThrownBy(() -> viewer.ownObservationByImage().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(views.rankPage(species, NaturalistName.of("someone-else")).orElseThrow().viewer().collected()).isFalse();
    }

    @Test
    void rankPage_unknownName_isEmpty() {
        assertThat(views.rankPage(TestInsectsIdentifiers.InsectSpecies.NotFound.name, FLORA)).isEmpty();
    }

    @Test
    void rankPage_rejectsNullName() {
        assertThatThrownBy(() -> views.rankPage(null, FLORA)).isInstanceOf(InvariantViolationException.class);
    }
    private void sighting(InsectSpeciesName name) {
        context.insectCommand().sightings().record(name, FLORA, null);
    }

    @Test
    void speciesCards_firstPage_carriesLabelsRoleAndImages() {
        InsectViews.SpeciesCard card = views.speciesCards(PageRequest.first(100), null).content().stream()
                .filter(c -> c.species().name().equals(species)).findFirst().orElseThrow();

        assertThat(card.collected()).isFalse();
        assertThat(card.genus()).isNotNull();
        assertThat(card.binomial()).isEqualTo(card.species().binomial(card.genus()));
        assertThat(card.displayName()).isEqualTo(card.species().displayName(card.genus()));
        assertThat(card.genusLabel()).isEqualTo(card.genus().genus().value());
        assertThat(card.familyLabel()).isNotEqualTo("\u2014");
        assertThat(card.role()).isNotNull();
        assertThat(card.images().stream().toList()).isNotEmpty();
    }

    @Test
    void speciesCards_withoutCommonName_displaysTheBinomialNotTheSlug() {
        InsectViews.SpeciesCard card = views.speciesCards(PageRequest.first(100), null).content().stream()
                .filter(c -> c.species().name().equals(TestInsectsIdentifiers.InsectSpecies.DrosophilaFunebris.name))
                .findFirst().orElseThrow();

        assertThat(card.species().commonNames()).isEmpty();
        assertThat(card.genus()).isNotNull();
        assertThat(card.displayName()).isEqualTo(card.binomial()).isNotEqualTo(card.species().name().value());
    }

    @Test
    void speciesCards_emptyPage_isEmpty() {
        assertThat(views.speciesCards(PageRequest.of(999, 25), FLORA).content()).isEmpty();
    }

    @Test
    void speciesCards_withViewer_marksCollected() {
        sighting(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        Page<InsectViews.SpeciesCard> page = views.speciesCards(PageRequest.first(100), FLORA);

        assertThat(page.content()).filteredOn(InsectViews.SpeciesCard::collected)
                .extracting(c -> c.species().name()).containsExactly(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
        assertThat(page.content()).hasSizeGreaterThan(1);
    }

    @Test
    void collectedBy_returnsOnlyObservedSpecies() {
        sighting(TestInsectsIdentifiers.InsectSpecies.ColiasEurytheme.name);
        sighting(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        Page<InsectViews.SpeciesCard> page = views.collectedBy(FLORA, PageRequest.first(10));

        assertThat(page.content()).extracting(c -> c.species().name())
                .containsExactly(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name, TestInsectsIdentifiers.InsectSpecies.ColiasEurytheme.name);
        assertThat(page.content()).allMatch(InsectViews.SpeciesCard::collected);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void collectedBy_pagesOverTheCollection() {
        sighting(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
        sighting(TestInsectsIdentifiers.InsectSpecies.ColiasEurytheme.name);
        sighting(TestInsectsIdentifiers.InsectSpecies.HippodamiaConvergens.name);

        Page<InsectViews.SpeciesCard> first = views.collectedBy(FLORA, PageRequest.of(0, 2, 2));
        Page<InsectViews.SpeciesCard> second = views.collectedBy(FLORA, PageRequest.of(1, 2, 2));

        assertThat(first.content()).hasSize(2);
        assertThat(first.pagesAheadKnown()).isEqualTo(1);
        assertThat(first.moreBeyondLookahead()).isFalse();
        assertThat(second.content()).hasSize(1);
        assertThat(second.hasNext()).isFalse();
        assertThat(second.pageNumber()).isEqualTo(1);
    }

    @Test
    void collectedBy_viewerWithNoObservations_isAnEmptyLastPage() {
        Page<InsectViews.SpeciesCard> page = views.collectedBy(FLORA, PageRequest.first(10));

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void collectedBy_hugePageNumber_isEmptyNotAnOverflow() {
        sighting(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        Page<InsectViews.SpeciesCard> page = views.collectedBy(FLORA, PageRequest.of(100_000_000, 25));

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void speciesCards_rejectsNullRequest() {
        assertThatThrownBy(() -> views.speciesCards(null, FLORA)).isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void orderCards_rejectsNullRequest() {
        assertThatThrownBy(() -> views.orderCards(null)).isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void familyCards_carryOrderLabelAndSubtreeImages() {
        Page<InsectViews.RankCard> page = views.familyCards(PageRequest.first(100));
        assertThat(page.content()).isNotEmpty();
        Map<InsectFamilyName, InsectFamilyInfo> families = context.insectQuery().families()
                .findByNameSet(page.content().stream().map(c -> (InsectFamilyName) c.name()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(InsectFamilyInfo::name, Function.identity()));
        Map<InsectOrderName, InsectOrderInfo> orders = context.insectQuery().orders()
                .findByNameSet(families.values().stream().map(InsectFamilyInfo::orderName).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(InsectOrderInfo::name, Function.identity()));
        for (InsectViews.RankCard card : page.content()) {
            InsectFamilyInfo family = families.get((InsectFamilyName) card.name());
            assertThat(card.displayName()).isEqualTo(family.displayName());
            assertThat(card.scientificName()).isEqualTo(family.family().value());
            assertThat(card.parentName()).isEqualTo(family.orderName());
            assertThat(card.parentLabel()).isEqualTo(orders.get(family.orderName()).order().value());
        }
        assertThat(page.content()).anyMatch(c -> !c.images().stream().toList().isEmpty());
    }

    @Test
    void familyCards_emptyPage_isEmpty() {
        assertThat(views.familyCards(PageRequest.of(999, 25)).content()).isEmpty();
    }

    @Test
    void genusCards_carryFamilyLabel() {
        Page<InsectViews.RankCard> page = views.genusCards(PageRequest.first(100));
        assertThat(page.content()).isNotEmpty();
        Map<InsectGenusName, InsectGenusInfo> genera = context.insectQuery().genera()
                .findByNameSet(page.content().stream().map(c -> (InsectGenusName) c.name()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(InsectGenusInfo::name, Function.identity()));
        Map<InsectFamilyName, InsectFamilyInfo> families = context.insectQuery().families()
                .findByNameSet(genera.values().stream().map(InsectGenusInfo::familyName).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(InsectFamilyInfo::name, Function.identity()));
        for (InsectViews.RankCard card : page.content()) {
            InsectGenusInfo genus = genera.get((InsectGenusName) card.name());
            assertThat(card.displayName()).isEqualTo(genus.displayName());
            assertThat(card.scientificName()).isEqualTo(genus.genus().value());
            assertThat(card.parentName()).isEqualTo(genus.familyName());
            assertThat(card.parentLabel()).isEqualTo(families.get(genus.familyName()).family().value());
        }
    }

    @Test
    void orderCards_haveNoParent() {
        Page<InsectViews.RankCard> page = views.orderCards(PageRequest.first(100));
        assertThat(page.content()).isNotEmpty();
        Map<InsectOrderName, InsectOrderInfo> orders = context.insectQuery().orders()
                .findByNameSet(page.content().stream().map(c -> (InsectOrderName) c.name()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(InsectOrderInfo::name, Function.identity()));
        for (InsectViews.RankCard card : page.content()) {
            InsectOrderInfo order = orders.get((InsectOrderName) card.name());
            assertThat(card.displayName()).isEqualTo(order.displayName());
            assertThat(card.scientificName()).isEqualTo(order.order().value());
            assertThat(card.parentName()).isNull();
            assertThat(card.parentLabel()).isNull();
        }
        assertThat(page.content()).anyMatch(c -> !c.images().stream().toList().isEmpty());
    }
}
