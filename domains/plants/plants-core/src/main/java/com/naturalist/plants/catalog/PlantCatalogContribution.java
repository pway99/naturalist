package com.naturalist.plants.catalog;

import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.plants.Plant;
import com.naturalist.plants.PlantEntityCollections.PlantCollection;
import com.naturalist.plants.PlantQuery;
import com.naturalist.plants.PlantsDomain;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicSpecies;

import java.util.stream.Collectors;
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

    private final PlantQuery.PlantEntityQuery plants;

    public PlantCatalogContribution(PlantQuery.PlantEntityQuery plants) {
        Observer.forClass(PlantCatalogContribution.class)
                .arguments("constructor", i -> i.notNull(plants, "plants"))
                .throwWhenInvalid();
        this.plants = plants;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Stream<SearchableEntity> searchableEntities() {
        var names = plants.allPlantNames();
        if (names.isEmpty()) {
            return Stream.empty();
        }
        PlantCollection collection = plants.findByNameSet(names.stream().collect(Collectors.toSet()));
        return collection.stream().map(PlantCatalogContribution::toSearchableEntity);
    }

    private static SearchableEntity toSearchableEntity(Plant plant) {
        EntityRef target = new EntityRef(DOMAIN, plant.name());
        return new SearchableEntity(target, tokensFor(plant));
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
}
