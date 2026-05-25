package com.naturalist.insects.catalog;

import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.Pages;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.*;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicGenus;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forward-direction catalog contribution for the insects domain — emits one
 * {@link SearchableEntity} per {@link InsectSpecies} in the live catalog with
 * the tokens under which a young naturalist might search:
 *
 * <ul>
 *   <li>the species slug (e.g. {@code "hippodamia-convergens"}) — the kernel
 *       indexes this separately so an exact slug hit reports as
 *       {@link com.naturalist.catalog.MatchKind#EXACT_SLUG};</li>
 *   <li>the genus alone when known (e.g. {@code "Hippodamia"});</li>
 *   <li>the scientific binomial when both genus and species are known
 *       (e.g. {@code "Hippodamia convergens"});</li>
 *   <li>the abbreviated binomial when both genus and species are known
 *       (e.g. {@code "H. convergens"});</li>
 *   <li>each {@link CommonName}'s label (e.g. {@code "Convergent Ladybug"})
 *       — locale is a presentation concern, not a search concern, so it
 *       is dropped here.</li>
 * </ul>
 *
 * <h2>Token collisions are normal</h2>
 * Per the redirect plan's M4′ entry, this contribution emits every
 * derivable token unconditionally — ambiguity filtering is the search
 * index's concern, not the contribution's.
 *
 * <h2>Live derivation</h2>
 * {@link #searchableEntities()} returns a fresh stream on every call,
 * reading from the underlying {@link InsectQuery.SpeciesQuery}. Species
 * added to the catalog after assembly are reflected automatically.
 */
@DomainService
public class InsectsCatalogContribution implements CatalogContribution {

    private static final DomainId DOMAIN = new InsectsDomain();

    /**
     * Page size for catalog assembly — bulk read, no horizon needed.
     */
    private static final int ASSEMBLY_PAGE_SIZE = 1000;

    private final InsectQuery.SpeciesQuery species;
    private final InsectQuery.FamilyQuery families;
    private final InsectQuery.GenusQuery genera;
    private final InsectQuery.OrderQuery orders;

    public InsectsCatalogContribution(InsectQuery.SpeciesQuery species,
                                      InsectQuery.FamilyQuery families,
                                      InsectQuery.GenusQuery genera,
                                      InsectQuery.OrderQuery orders) {
        Observer.forClass(InsectsCatalogContribution.class)
                .arguments("constructor", i -> i
                        .notNull(species, "species")
                        .notNull(families, "families")
                        .notNull(genera, "genera")
                        .notNull(orders, "orders"))
                .throwWhenInvalid();
        this.species = species;
        this.families = families;
        this.genera = genera;
        this.orders = orders;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Stream<SearchableEntity> searchableEntities() {
        // Build the genus lookup once per stream so species token assembly can resolve
        // the parent genus's TaxonomicGenus epithet without re-querying per species.
        Map<InsectGenusName, InsectGenus> genusByName = Pages.stream(ASSEMBLY_PAGE_SIZE, genera::findPage)
                .collect(Collectors.toMap(InsectGenus::name, Function.identity()));
        return Stream.of(
                speciesEntities(genusByName),
                familyEntities(),
                genusEntities(),
                orderEntities()
        ).flatMap(s -> s);
    }

    private Stream<SearchableEntity> speciesEntities(Map<InsectGenusName, InsectGenus> genusByName) {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, species::findPage)
                .map(s -> toSearchableSpecies(s, genusByName));
    }

    private Stream<SearchableEntity> familyEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, families::findPage)
                .map(InsectsCatalogContribution::toSearchableFamily);
    }

    private Stream<SearchableEntity> genusEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, genera::findPage)
                .map(InsectsCatalogContribution::toSearchableGenus);
    }

    private static SearchableEntity toSearchableSpecies(InsectSpecies entity,
                                                        Map<InsectGenusName, InsectGenus> genusByName) {
        EntityRef target = new EntityRef(DOMAIN, entity.name());
        return new SearchableEntity(target, tokensFor(entity, genusByName));
    }

    private static SearchableEntity toSearchableFamily(InsectFamily entity) {
        EntityRef target = new EntityRef(DOMAIN, entity.name());
        return new SearchableEntity(target, tokensFor(entity));
    }

    private static SearchableEntity toSearchableGenus(InsectGenus entity) {
        EntityRef target = new EntityRef(DOMAIN, entity.name());
        return new SearchableEntity(target, tokensFor(entity));
    }

    private static Stream<String> tokensFor(InsectSpecies entity,
                                            Map<InsectGenusName, InsectGenus> genusByName) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(entity.name().value());
        // Every species has a non-null genusName + epithet after the TaxonomicClassification
        // collapse — referential integrity to the parent genus record is enforced by the
        // species's invariants. A missing parent here would be a catalog data error.
        InsectGenus parentGenus = genusByName.get(entity.genusName());
        if (parentGenus != null) {
            TaxonomicGenus genusEpithet = parentGenus.genus();
            String speciesEpithet = entity.epithet().value();
            tokens.add(genusEpithet.value());
            tokens.add(genusEpithet.value() + " " + speciesEpithet);
            tokens.add(genusEpithet.value().charAt(0) + ". " + speciesEpithet);
        }
        entity.commonNames().forEach(commonName -> tokens.add(commonName.label()));
        return tokens.build();
    }

    private static Stream<String> tokensFor(InsectFamily entity) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(entity.name().value());
        tokens.add(entity.family().value());
        entity.commonNames().forEach(commonName -> tokens.add(commonName.label()));
        return tokens.build();
    }

    private static Stream<String> tokensFor(InsectGenus entity) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(entity.name().value());
        tokens.add(entity.genus().value());
        entity.commonNames().forEach(commonName -> tokens.add(commonName.label()));
        return tokens.build();
    }

    private Stream<SearchableEntity> orderEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, orders::findPage)
                .map(InsectsCatalogContribution::toSearchableOrder);
    }

    private static SearchableEntity toSearchableOrder(InsectOrder entity) {
        EntityRef target = new EntityRef(DOMAIN, entity.name());
        return new SearchableEntity(target, tokensFor(entity));
    }

    private static Stream<String> tokensFor(InsectOrder entity) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(entity.name().value());
        tokens.add(entity.order().value());
        entity.commonNames().forEach(commonName -> tokens.add(commonName.label()));
        return tokens.build();
    }
}
