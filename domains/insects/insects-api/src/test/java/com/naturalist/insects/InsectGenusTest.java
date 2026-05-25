package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Papilionidae;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.LightRegime;
import com.naturalist.habitat.MoistureRegime;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.StageHabitat;
import com.naturalist.insects.lifestage.StagePhenology;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicGenus;
import org.junit.jupiter.api.Test;

import java.time.MonthDay;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectGenusTest {
    private static final Observer observer = Observer.forClass(InsectGenusTest.class);

    @Test
    void genusWithLarvaAtGenusRankIsValidAndCarriesCompositeSlug() {
        var mo = observer.forMethod("genusWithLarvaAtGenusRankIsValidAndCarriesCompositeSlug");
        InsectGenusName name = InsectGenusName.of("chrysoperla");
        LarvaStage larva = new LarvaStage(
                LifeStageName.of(name, LifeStageKind.LARVA),
                phenology(),
                habitat(),
                null,
                description(),
                null,
                List.of(),
                List.of(),
                null,
                null);

        InsectGenus genus = new InsectGenus(
                name,
                InsectFamilyName.of("chrysopidae"),
                InsectOrderName.of("neuroptera"),
                TaxonomicGenus.of("Chrysoperla"),
                description(),
                Set.of(),
                null,
                null,
                larva,
                null,
                null);

        InvariantObservation result = mo.namedEntity(genus, "genus");

        assertThat(result.violations()).isEmpty();
        assertThat(genus.larva().name().value()).isEqualTo("chrysoperla-larva");
        assertThat(genus.egg()).isNull();
        assertThat(genus.pupa()).isNull();
        assertThat(genus.adult()).isNull();
    }

    @Test
    void withLarvaReturnsNewInstanceCarryingTheStage() {
        InsectGenusName name = InsectGenusName.of("chrysoperla");
        InsectGenus genus = new InsectGenus(
                name,
                InsectFamilyName.of("chrysopidae"),
                InsectOrderName.of("neuroptera"),
                TaxonomicGenus.of("Chrysoperla"),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
        LarvaStage larva = new LarvaStage(
                LifeStageName.of(name, LifeStageKind.LARVA),
                phenology(),
                habitat(),
                null,
                description(),
                null,
                List.of(),
                List.of(),
                null,
                null);

        InsectGenus updated = genus.withLarva(larva);

        assertThat(updated.larva()).isEqualTo(larva);
        assertThat(genus.larva()).isNull();
    }

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectGenus genus = genusWithPlacedIn(null);

        InsectGenus updated = genus.withPlacedIn(new Papilionidae());

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
        assertThat(genus.placedIn()).isNull();
    }

    @Test
    void withLarvaPreservesPlacedIn() {
        InsectGenus genus = genusWithPlacedIn(new Papilionidae());

        InsectGenus updated = genus.withLarva(null);

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
    }

    private static InsectGenus genusWithPlacedIn(Clade placedIn) {
        return new InsectGenus(
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicGenus.of("Battus"),
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
