package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Papilionidae;
import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.LightRegime;
import com.naturalist.habitat.MoistureRegime;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.StageHabitat;
import com.naturalist.insects.lifestage.StagePhenology;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.junit.jupiter.api.Test;

import java.time.MonthDay;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectFamilyTest {
    private static final Observer observer = Observer.forClass(InsectFamilyTest.class);

    @Test
    void familyWithAdultAtFamilyRankIsValidAndCarriesCompositeSlug() {
        var mo = observer.forMethod("familyWithAdultAtFamilyRankIsValidAndCarriesCompositeSlug");
        InsectFamilyName name = InsectFamilyName.of("syrphidae");
        AdultStage adult = new AdultStage(
                LifeStageName.of(name, LifeStageKind.ADULT),
                phenology(),
                habitat(),
                null,
                description(),
                null,
                List.of(),
                null,
                null);

        InsectFamily family = new InsectFamily(
                name,
                TaxonomicOrder.of("Diptera"),
                TaxonomicFamily.of("Syrphidae"),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                adult);

        InvariantObservation result = mo.namedEntity(family, "family");

        assertThat(result.violations()).isEmpty();
        assertThat(family.adult().name().value()).isEqualTo("syrphidae-adult");
        assertThat(family.egg()).isNull();
        assertThat(family.larva()).isNull();
        assertThat(family.pupa()).isNull();
    }

    @Test
    void withAdultReturnsNewInstanceCarryingTheStage() {
        InsectFamilyName name = InsectFamilyName.of("syrphidae");
        InsectFamily family = new InsectFamily(
                name,
                TaxonomicOrder.of("Diptera"),
                TaxonomicFamily.of("Syrphidae"),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
        AdultStage adult = new AdultStage(
                LifeStageName.of(name, LifeStageKind.ADULT),
                phenology(),
                habitat(),
                null,
                description(),
                null,
                List.of(),
                null,
                null);

        InsectFamily updated = family.withAdult(adult);

        assertThat(updated.adult()).isEqualTo(adult);
        assertThat(family.adult()).isNull();
    }

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectFamily family = familyWithPlacedIn(null);

        InsectFamily updated = family.withPlacedIn(new Papilionidae());

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
        assertThat(family.placedIn()).isNull();
    }

    @Test
    void withEggPreservesPlacedIn() {
        InsectFamily family = familyWithPlacedIn(new Papilionidae());

        InsectFamily updated = family.withEgg(null);

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
    }

    private static InsectFamily familyWithPlacedIn(Clade placedIn) {
        return new InsectFamily(
                InsectFamilyName.of("papilionidae"),
                TaxonomicOrder.of("Lepidoptera"),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                placedIn,
                null,
                null,
                null,
                null);
    }

    private static StagePhenology phenology() {
        return new StagePhenology(
                List.of(new StagePhenology.ActivityWindow(
                        MonthDay.of(4, 1), MonthDay.of(6, 15), MonthDay.of(10, 15), null)),
                null);
    }

    private static StageHabitat habitat() {
        return new StageHabitat(
                new HabitatProfile(
                        Set.of(HabitatZone.CULTIVATED),
                        MoistureRegime.MESIC,
                        LightRegime.PARTIAL_SUN,
                        null),
                null, null, null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
