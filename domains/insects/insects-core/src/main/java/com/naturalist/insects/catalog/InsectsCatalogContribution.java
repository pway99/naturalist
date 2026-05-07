package com.naturalist.insects.catalog;

import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.Pages;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.*;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicSpecies;

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

    public InsectsCatalogContribution(InsectQuery.SpeciesQuery species,
                                      InsectQuery.FamilyQuery families,
                                      InsectQuery.GenusQuery genera) {
        Observer.forClass(InsectsCatalogContribution.class)
                .arguments("constructor", i -> i
                        .notNull(species, "species")
                        .notNull(families, "families")
                        .notNull(genera, "genera"))
                .throwWhenInvalid();
        this.species = species;
        this.families = families;
        this.genera = genera;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Stream<SearchableEntity> searchableEntities() {
        return Stream.of(speciesEntities(), familyEntities(), genusEntities())
                .flatMap(s -> s);
    }

    private Stream<SearchableEntity> speciesEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, species::findPage)
                .map(InsectsCatalogContribution::toSearchableSpecies);
    }

    private Stream<SearchableEntity> familyEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, families::findPage)
                .map(InsectsCatalogContribution::toSearchableFamily);
    }

    private Stream<SearchableEntity> genusEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, genera::findPage)
                .map(InsectsCatalogContribution::toSearchableGenus);
    }

    private static SearchableEntity toSearchableSpecies(InsectSpecies entity) {
        EntityRef target = new EntityRef(DOMAIN, entity.name());
        return new SearchableEntity(target, tokensFor(entity));
    }

    private static SearchableEntity toSearchableFamily(InsectFamily entity) {
        EntityRef target = new EntityRef(DOMAIN, entity.name());
        return new SearchableEntity(target, tokensFor(entity));
    }

    private static SearchableEntity toSearchableGenus(InsectGenus entity) {
        EntityRef target = new EntityRef(DOMAIN, entity.name());
        return new SearchableEntity(target, tokensFor(entity));
    }

    private static Stream<String> tokensFor(InsectSpecies entity) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(entity.name().value());
        TaxonomicClassification taxonomy = entity.taxonomy();
        TaxonomicGenus genus = taxonomy.genus();
        TaxonomicSpecies speciesEpithet = taxonomy.species();
        if (genus != null) {
            tokens.add(genus.value());
            if (speciesEpithet != null) {
                tokens.add(genus.value() + " " + speciesEpithet.value());
                tokens.add(genus.value().charAt(0) + ". " + speciesEpithet.value());
            }
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
}
