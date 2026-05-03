package com.naturalist.insects.catalog;

import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;
import com.naturalist.insects.InsectQuery;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.InsectsDomain;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicSpecies;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forward-direction catalog contribution for the insects domain — emits one
 * {@link SearchableEntity} per {@link InsectSpecies} in the live catalog with
 * the tokens under which a young naturalist might search:
 *
 * <ul>
 *   <li>the species slug (e.g. {@code "convergent-ladybug"}) — the kernel
 *       indexes this separately so an exact slug hit reports as
 *       {@link com.naturalist.catalog.MatchKind#EXACT_SLUG};</li>
 *   <li>the genus alone when known (e.g. {@code "Hippodamia"});</li>
 *   <li>the scientific binomial when both genus and species are known
 *       (e.g. {@code "Hippodamia convergens"});</li>
 *   <li>the abbreviated binomial when both genus and species are known
 *       (e.g. {@code "H. convergens"}).</li>
 * </ul>
 *
 * <h2>No common-name tokens (yet)</h2>
 * {@link InsectSpecies} does not currently carry a {@code Set<CommonName>}
 * field; the contribution emits taxonomic tokens only. When common names
 * are added to the species record they should be folded into
 * {@link #tokensFor} alongside the binomial forms — same pattern as
 * {@code PlantCatalogContribution}.
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

    private final InsectQuery.SpeciesQuery species;

    public InsectsCatalogContribution(InsectQuery.SpeciesQuery species) {
        Observer.forClass(InsectsCatalogContribution.class)
                .arguments("constructor", i -> i.notNull(species, "species"))
                .throwWhenInvalid();
        this.species = species;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Stream<SearchableEntity> searchableEntities() {
        var names = species.allSpeciesNames();
        if (names.isEmpty()) {
            return Stream.empty();
        }
        SpeciesCollection collection =
                species.findByNameSet(names.stream().collect(Collectors.toSet()));
        return collection.stream().map(InsectsCatalogContribution::toSearchableEntity);
    }

    private static SearchableEntity toSearchableEntity(InsectSpecies entity) {
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
        return tokens.build();
    }
}
