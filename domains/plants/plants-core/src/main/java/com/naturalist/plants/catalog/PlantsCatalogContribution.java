package com.naturalist.plants.catalog;

import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.Pages;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.plants.*;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forward-direction catalog contribution for the plants domain — emits one
 * {@link SearchableEntity} per catalogued plant taxon — order, family, genus and
 * {@link PlantSpecies} — with the tokens under which a young naturalist might search:
 *
 * <ul>
 *   <li>the plant slug (e.g. {@code "aristolochia-californica"}) — the kernel
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
public class PlantsCatalogContribution implements CatalogContribution {

    private static final DomainId DOMAIN = new PlantsDomain();

    /**
     * Page size for catalog assembly — bulk read, no horizon needed.
     * The pager is lookahead-0 (single page query, no probe), so the cost
     * per page is one round trip with bounded payload.
     */
    private static final int ASSEMBLY_PAGE_SIZE = 1000;

    private final PlantQuery.PlantEntityQuery plants;
    private final PlantQuery.PlantOrderEntityQuery orders;
    private final PlantQuery.PlantFamilyEntityQuery families;
    private final PlantQuery.PlantGenusEntityQuery genera;

    public PlantsCatalogContribution(PlantQuery.PlantEntityQuery plants,
                                    PlantQuery.PlantOrderEntityQuery orders,
                                    PlantQuery.PlantFamilyEntityQuery families,
                                    PlantQuery.PlantGenusEntityQuery genera) {
        Observer.forClass(PlantsCatalogContribution.class)
                .arguments("constructor", i -> i
                        .notNull(plants, "plants")
                        .notNull(orders, "orders")
                        .notNull(families, "families")
                        .notNull(genera, "genera"))
                .throwWhenInvalid();
        this.plants = plants;
        this.orders = orders;
        this.families = families;
        this.genera = genera;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Stream<SearchableEntity> searchableEntities() {
        return Stream.of(plantEntities(), orderEntities(), familyEntities(), genusEntities())
                .flatMap(s -> s);
    }

    /**
     * The binomial tokens need the genus <em>epithet</em> ({@code "Aristolochia"}), while a
     * species carries only its parent's <em>slug</em> ({@code "aristolochia"}). Resolving
     * the epithet from the slug by capitalising would be a guess; the genus record holds
     * the authored form, so the epithets are indexed once per stream and looked up per
     * plant. A species whose genus is missing still contributes its slug and common names —
     * degraded, not absent.
     */
    private Stream<SearchableEntity> plantEntities() {
        Map<String, String> genusEpithets = Pages.stream(ASSEMBLY_PAGE_SIZE, genera::findPage)
                .collect(Collectors.toMap(g -> g.name().value(), g -> g.genus().value()));
        return Pages.stream(ASSEMBLY_PAGE_SIZE, plants::findPage)
                .map(plant -> toSearchablePlant(
                        plant, genusEpithets.get(plant.genusName().value())));
    }

    private Stream<SearchableEntity> orderEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, orders::findPage)
                .map(PlantsCatalogContribution::toSearchableOrder);
    }

    private Stream<SearchableEntity> familyEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, families::findPage)
                .map(PlantsCatalogContribution::toSearchableFamily);
    }

    private Stream<SearchableEntity> genusEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, genera::findPage)
                .map(PlantsCatalogContribution::toSearchableGenus);
    }

    private static SearchableEntity toSearchablePlant(PlantSpecies plant, String genusEpithet) {
        EntityRef target = new EntityRef(DOMAIN, plant.name());
        return new SearchableEntity(target, tokensFor(plant, genusEpithet));
    }

    private static SearchableEntity toSearchableOrder(PlantOrder orderEntity) {
        EntityRef target = new EntityRef(DOMAIN, orderEntity.name());
        return new SearchableEntity(target, tokensFor(orderEntity));
    }

    private static SearchableEntity toSearchableFamily(PlantFamily family) {
        EntityRef target = new EntityRef(DOMAIN, family.name());
        return new SearchableEntity(target, tokensFor(family));
    }

    private static SearchableEntity toSearchableGenus(PlantGenus genusEntity) {
        EntityRef target = new EntityRef(DOMAIN, genusEntity.name());
        return new SearchableEntity(target, tokensFor(genusEntity));
    }

    private static Stream<String> tokensFor(PlantSpecies plant, String genusEpithet) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(plant.name().value());
        if (genusEpithet != null && !genusEpithet.isBlank()) {
            String species = plant.epithet().value();
            tokens.add(genusEpithet);
            tokens.add(genusEpithet + " " + species);
            tokens.add(genusEpithet.charAt(0) + ". " + species);
        }
        plant.commonNames().forEach(commonName -> tokens.add(commonName.label()));
        return tokens.build();
    }

    private static Stream<String> tokensFor(PlantOrder orderEntity) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(orderEntity.name().value());
        tokens.add(orderEntity.order().value());
        orderEntity.commonNames().forEach(commonName -> tokens.add(commonName.label()));
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
