package com.naturalist.plants.catalog;

import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.Pages;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.plants.*;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicSpecies;

import java.util.stream.Stream;

/**
 * Forward-direction catalog contribution for the plants domain — emits one
 * {@link SearchableEntity} per {@link Plant} in the live catalog with the
 * tokens under which a young naturalist might search:
 *
 * <ul>
 *   <li>the plant slug (e.g. {@code "california-pipevine"}) — the kernel
 *       indexes this separately so an exact slug hit reports as
 *       {@link com.naturalist.catalog.MatchKind#EXACT_SLUG};</li>
 *   <li>the scientific binomial when both genus and species are known
 *       (e.g. {@code "Aristolochia californica"});</li>
 *   <li>the genus alone (e.g. {@code "Aristolochia"});</li>
 *   <li>the abbreviated binomial when both genus and species are known
 *       (e.g. {@code "A. californica"});</li>
 *   <li>each {@link CommonName}'s label (e.g. {@code "California Pipevine"},
 *       {@code "pipevine"}) — locale is a presentation concern, not a search
 *       concern, so it is dropped here.</li>
 * </ul>
 *
 * <h2>Token collisions are normal</h2>
 * Per the redirect plan's M4′ entry, this contribution emits every derivable
 * token unconditionally. Two {@code Trifolium} species both emit
 * {@code "Trifolium"} as a genus token; the kernel indexes both, and a search
 * for the genus returns both species — exactly the desired behaviour for a
 * search-and-discovery surface.
 *
 * <h2>Live derivation</h2>
 * {@link #searchableEntities()} returns a fresh stream on every call, reading
 * from the underlying {@link PlantQuery.PlantEntityQuery}. Plants added to the
 * catalog after assembly are reflected automatically when the kernel iterates
 * the stream.
 */
@DomainService
public class PlantCatalogContribution implements CatalogContribution {

    private static final DomainId DOMAIN = new PlantsDomain();

    /**
     * Page size for catalog assembly — bulk read, no horizon needed.
     * The pager is lookahead-0 (single page query, no probe), so the cost
     * per page is one round trip with bounded payload.
     */
    private static final int ASSEMBLY_PAGE_SIZE = 1000;

    private final PlantQuery.PlantEntityQuery plants;
    private final PlantQuery.PlantFamilyEntityQuery families;
    private final PlantQuery.PlantGenusEntityQuery genera;

    public PlantCatalogContribution(PlantQuery.PlantEntityQuery plants,
                                    PlantQuery.PlantFamilyEntityQuery families,
                                    PlantQuery.PlantGenusEntityQuery genera) {
        Observer.forClass(PlantCatalogContribution.class)
                .arguments("constructor", i -> i
                        .notNull(plants, "plants")
                        .notNull(families, "families")
                        .notNull(genera, "genera"))
                .throwWhenInvalid();
        this.plants = plants;
        this.families = families;
        this.genera = genera;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Stream<SearchableEntity> searchableEntities() {
        return Stream.of(plantEntities(), familyEntities(), genusEntities())
                .flatMap(s -> s);
    }

    private Stream<SearchableEntity> plantEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, plants::findPage)
                .map(PlantCatalogContribution::toSearchablePlant);
    }

    private Stream<SearchableEntity> familyEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, families::findPage)
                .map(PlantCatalogContribution::toSearchableFamily);
    }

    private Stream<SearchableEntity> genusEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, genera::findPage)
                .map(PlantCatalogContribution::toSearchableGenus);
    }

    private static SearchableEntity toSearchablePlant(Plant plant) {
        EntityRef target = new EntityRef(DOMAIN, plant.name());
        return new SearchableEntity(target, tokensFor(plant));
    }

    private static SearchableEntity toSearchableFamily(PlantFamily family) {
        EntityRef target = new EntityRef(DOMAIN, family.name());
        return new SearchableEntity(target, tokensFor(family));
    }

    private static SearchableEntity toSearchableGenus(PlantGenus genusEntity) {
        EntityRef target = new EntityRef(DOMAIN, genusEntity.name());
        return new SearchableEntity(target, tokensFor(genusEntity));
    }

    private static Stream<String> tokensFor(Plant plant) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(plant.name().value());
        TaxonomicClassification taxonomy = plant.taxonomy();
        TaxonomicGenus genus = taxonomy.genus();
        TaxonomicSpecies species = taxonomy.species();
        if (genus != null) {
            tokens.add(genus.value());
            if (species != null) {
                tokens.add(genus.value() + " " + species.value());
                tokens.add(genus.value().charAt(0) + ". " + species.value());
            }
        }
        plant.commonNames().forEach(commonName -> tokens.add(commonName.label()));
        return tokens.build();
    }

    private static Stream<String> tokensFor(PlantFamily family) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(family.name().value());
        tokens.add(family.family().value());
        family.commonNames().forEach(commonName -> tokens.add(commonName.label()));
        return tokens.build();
    }

    private static Stream<String> tokensFor(PlantGenus genusEntity) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(genusEntity.name().value());
        tokens.add(genusEntity.genus().value());
        genusEntity.commonNames().forEach(commonName -> tokens.add(commonName.label()));
        return tokens.build();
    }
}
