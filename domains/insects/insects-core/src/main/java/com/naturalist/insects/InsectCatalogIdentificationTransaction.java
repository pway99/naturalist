package com.naturalist.insects;

import com.naturalist.data.Transaction;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicClassification;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Persists a {@link CatalogIdentification} aggregate atomically — ensuring
 * the full Linnaean path up to the identified rank exists before inserting
 * the image and field observation.
 *
 * <p>Idempotent for the identified entity: if it already exists in the catalog,
 * it is left untouched and only the image, observation, and new features are
 * inserted. Parent ranks follow the same pattern — existing ranks are never
 * overwritten.
 *
 * <p>Parent-rank resolution is conditional on the identified rank: a family-level
 * identification resolves only the parent order, not the family or genus itself
 * (those are the identified entity's concern). A species-level identification
 * resolves order → family → genus before inserting the species.
 */
class InsectCatalogIdentificationTransaction extends Transaction<CatalogIdentification> {

    private static final Description PLACEHOLDER = new Description(
            "Identified via vision — description pending.",
            "Identified via vision — description pending.",
            "Identified via vision — description pending.",
            "Identified via vision — description pending.");

    private final InsectCommand insectCommand;
    private final InsectQuery insectQuery;

    InsectCatalogIdentificationTransaction(InsectCommand insectCommand, InsectQuery insectQuery) {
        this.insectCommand = insectCommand;
        this.insectQuery = insectQuery;
    }

    @Override
    protected void doExecute(CatalogIdentification identification) {
        var taxonomy = identification.taxonomy();
        var identifiedEntity = identification.identifiedEntity();
        var parentDescriptions = identification.parentDescriptions();

        // 1. Insert identified rank entity if new — before parent resolution
        //    so the identified entity's real description is never shadowed
        //    by a placeholder parent-resolve insert
        insertIdentifiedEntity(identifiedEntity);

        // 2. Resolve parent ranks (order → family → genus) — only those
        //    ABOVE the identified rank. The identified rank itself was
        //    already inserted in step 1; resolve methods are idempotent
        //    so a duplicate hit is harmless.
        var orderName = resolveOrder(taxonomy, parentDescriptions);
        if (taxonomy.family() != null) {
            var familyName = resolveFamily(taxonomy, orderName, parentDescriptions);
            if (taxonomy.genus() != null) {
                resolveGenus(taxonomy, familyName, parentDescriptions);
            }
        }

        // 3. Insert image and observation
        insectCommand.images().insert(identification.image());
        insectCommand.fieldObservations().insert(identification.observation());

        // 4. Insert features and assignments
        for (var feature : identification.newFeatures()) {
            insectCommand.features().insert(feature);
        }
        for (var assignment : identification.featureAssignments()) {
            insectCommand.featureAssignments().insert(assignment);
        }
    }

    private void insertIdentifiedEntity(IdentifiedRankEntity entity) {
        switch (entity) {
            case IdentifiedRankEntity.Species(var species) -> {
                if (insectQuery.species().getByName(species.name()).isEmpty()) {
                    insectCommand.species().insert(species);
                }
            }
            case IdentifiedRankEntity.Genus(var genus) -> {
                if (insectQuery.genera().getByName(genus.name()).isEmpty()) {
                    insectCommand.genera().insert(genus);
                }
            }
            case IdentifiedRankEntity.Family(var family) -> {
                if (insectQuery.families().getByName(family.name()).isEmpty()) {
                    insectCommand.families().insert(family);
                }
            }
            case IdentifiedRankEntity.Order(var order) -> {
                if (insectQuery.orders().getByName(order.name()).isEmpty()) {
                    insectCommand.orders().insert(order);
                }
            }
        }
    }

    private InsectOrderName resolveOrder(TaxonomicClassification taxonomy,
                                         Map<InsectRankName, Description> parentDescriptions) {
        var orderSlug = taxonomy.order().value().toLowerCase(Locale.ROOT);
        var orderName = InsectOrderName.of(orderSlug);
        if (insectQuery.orders().getByName(orderName).isEmpty()) {
            var description = parentDescriptions.getOrDefault(orderName, PLACEHOLDER);
            insectCommand.orders().insert(new InsectOrder(
                    orderName, taxonomy.order(), description, Set.of(), null));
        }
        return orderName;
    }

    private InsectFamilyName resolveFamily(TaxonomicClassification taxonomy,
                                           InsectOrderName orderName,
                                           Map<InsectRankName, Description> parentDescriptions) {
        var familySlug = taxonomy.family().value().toLowerCase(Locale.ROOT);
        var familyName = InsectFamilyName.of(familySlug);
        if (insectQuery.families().getByName(familyName).isEmpty()) {
            var description = parentDescriptions.getOrDefault(familyName, PLACEHOLDER);
            insectCommand.families().insert(new InsectFamily(
                    familyName, orderName, taxonomy.family(), description, Set.of(), null));
        }
        return familyName;
    }

    private void resolveGenus(TaxonomicClassification taxonomy,
                              InsectFamilyName familyName,
                              Map<InsectRankName, Description> parentDescriptions) {
        var genusSlug = taxonomy.genus().value().toLowerCase(Locale.ROOT);
        var genusName = InsectGenusName.of(genusSlug);
        if (insectQuery.genera().getByName(genusName).isEmpty()) {
            var description = parentDescriptions.getOrDefault(genusName, PLACEHOLDER);
            insectCommand.genera().insert(new InsectGenus(
                    genusName, familyName, taxonomy.genus(), description, Set.of(), null));
        }
    }
}
