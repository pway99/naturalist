package com.naturalist.plants.console.catalog;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.plants.PlantFamilyName;
import com.naturalist.plants.PlantGenusName;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.plants.heritage.SeedLineageName;
import com.naturalist.plants.management.PlantProgramName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every plants-owned {@code EntityName} type must resolve to a URL.
 * <p>
 * {@code PlantCatalogContribution} indexes families and genera for search, but
 * the linker had no case for either, and
 * {@code SearchController.buildGroups} drops any hit whose linker returns
 * {@code null} — so family and genus hits were silently absent from results.
 * A missing case is invisible at runtime; this test is what makes it loud.
 */
class PlantsLinkerTest {

    // The linker only switches on ref.name(); the domain is irrelevant here.
    private record TestDomain() implements DomainId {
        @Override
        public String value() {
            return "plants";
        }
    }

    private final PlantsLinker linker = new PlantsLinker();

    private String link(EntityName name) {
        return linker.linkFor(new EntityRef(new TestDomain(), name));
    }

    @Test
    void linksPlantToPlantDetail() {
        assertThat(link(PlantSpeciesName.of("aristolochia-californica")))
                .isEqualTo("/plants/aristolochia-californica");
    }

    @Test
    void linksFamilyToFamilyDetail() {
        assertThat(link(PlantFamilyName.of("lamiaceae")))
                .isEqualTo("/plants/families/lamiaceae");
    }

    @Test
    void linksGenusToGenusDetail() {
        assertThat(link(PlantGenusName.of("thymus")))
                .isEqualTo("/plants/genera/thymus");
    }

    @Test
    void linksCultivarToCultivarDetail() {
        assertThat(link(CultivarName.of("amish-paste")))
                .isEqualTo("/plants/cultivars/amish-paste");
    }

    @Test
    void linksSeedLineageToLineageDetail() {
        assertThat(link(SeedLineageName.of("italian-pear-nicks")))
                .isEqualTo("/plants/lineages/italian-pear-nicks");
    }

    @Test
    void linksPlantProgramToProgramDetail() {
        assertThat(link(PlantProgramName.of("pipevine-pesticide-exclusion")))
                .isEqualTo("/plants/programs/pipevine-pesticide-exclusion");
    }

    @Test
    void linksConstituentToPhytochemistryDetail() {
        assertThat(link(PhytochemicalConstituentName.of("creeping-thyme-thymol")))
                .isEqualTo("/plants/phytochemistry/creeping-thyme-thymol");
    }

    @Test
    void returnsNullForNamesThisLinkerDoesNotOwn() {
        // Per the EntityRefLinker contract: null means "not mine", which lets
        // the composite fall through to the owning domain's linker.
        assertThat(link(new ForeignName("battus-philenor"))).isNull();
    }

    /** A name type from no plants sub-context. {@code EntityName} is an abstract class, not an interface. */
    private static final class ForeignName extends EntityName {

        private ForeignName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 64;
        }
    }
}
