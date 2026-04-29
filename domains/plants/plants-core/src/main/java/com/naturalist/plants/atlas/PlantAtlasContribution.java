package com.naturalist.plants.atlas;

import com.naturalist.atlas.AtlasContribution;
import com.naturalist.atlas.AtlasContribution.Alias;
import com.naturalist.atlas.DomainId;
import com.naturalist.atlas.EntityRef;
import com.naturalist.observability.Observer;
import com.naturalist.plants.Plant;
import com.naturalist.plants.PlantEntityCollections.PlantCollection;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.PlantQuery;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicSpecies;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forward-direction atlas contribution for the plants domain — the first real
 * producer in the plan's M4 milestone. Translates each {@link Plant} in the
 * live catalog into the surface forms a description renderer can recognise:
 *
 * <ul>
 *   <li>the plant slug (e.g. {@code "california-pipevine"});</li>
 *   <li>the scientific binomial when both genus and species are known
 *       (e.g. {@code "Aristolochia californica"});</li>
 *   <li>the genus alone (e.g. {@code "Aristolochia"});</li>
 *   <li>the abbreviated binomial when both genus and species are known
 *       (e.g. {@code "A. californica"}).</li>
 * </ul>
 *
 * <p>Per the plan's "Derive aliases, do not register" decision, no parallel
 * registration list exists — adding a {@link Plant} to the catalog
 * automatically grows the contribution.
 *
 * <h2>Common names</h2>
 * The plan's M4 entry calls for a {@code commonNames} pass-through if the
 * {@link Plant} record carries such a field. It does not yet (the field would
 * be a coordinated change across api modules per the plan's open question on
 * {@code commonNames} schema location), so common-name contribution is
 * deferred. When {@code Plant} grows a {@code commonNames} component the
 * lookup is a one-line addition to {@link #candidateForms(Plant)}.
 *
 * <h2>Ambiguity handling</h2>
 * The {@link com.naturalist.atlas.DefaultAtlas} rejects assembly when the same
 * surface form maps to different targets — necessary for routing safety, but
 * fatal for a derived contribution where the catalog naturally produces
 * collisions (two {@code Trifolium} species share the genus surface form,
 * for example). This contribution defends the assembly by collecting all
 * candidate forms, then emitting only those that resolve to a single target;
 * an ambiguous genus or abbreviated binomial is silently dropped from this
 * contribution rather than registered. The slug and full binomial are unique
 * by domain construction (slugs are the natural key; binomials are unique per
 * species in the plant catalog) and are always emitted when present.
 */
public class PlantAtlasContribution implements AtlasContribution {

    private static final DomainId DOMAIN = new DomainId.Plants();

    private final PlantQuery.PlantEntityQuery plants;

    public PlantAtlasContribution(PlantQuery.PlantEntityQuery plants) {
        Observer.forClass(PlantAtlasContribution.class)
                .arguments("constructor", i -> i.notNull(plants, "plants"))
                .throwWhenInvalid();
        this.plants = plants;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Stream<Alias> aliases() {
        Set<PlantName> names = plants.allPlantNames().stream().collect(Collectors.toSet());
        if (names.isEmpty()) {
            return Stream.empty();
        }
        PlantCollection collection = plants.findByNameSet(names);

        Map<String, Set<EntityRef>> bySurfaceForm = new LinkedHashMap<>();
        collection.stream().forEach(plant -> {
            EntityRef target = new EntityRef(DOMAIN, plant.name());
            candidateForms(plant).forEach(form ->
                    bySurfaceForm.computeIfAbsent(form, k -> new HashSet<>()).add(target));
        });

        return bySurfaceForm.entrySet().stream()
                .filter(e -> e.getValue().size() == 1)
                .map(e -> new Alias(e.getKey(), e.getValue().iterator().next()));
    }

    private static Stream<String> candidateForms(Plant plant) {
        Stream.Builder<String> builder = Stream.builder();
        builder.add(plant.name().value());
        TaxonomicClassification taxonomy = plant.taxonomy();
        TaxonomicGenus genus = taxonomy.genus();
        TaxonomicSpecies species = taxonomy.species();
        if (genus != null) {
            builder.add(genus.value());
            if (species != null) {
                builder.add(genus.value() + " " + species.value());
                builder.add(genus.value().charAt(0) + ". " + species.value());
            }
        }
        return builder.build();
    }
}
