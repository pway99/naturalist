package com.naturalist.insects;

import com.naturalist.data.Transaction;
import com.naturalist.fieldnotes.Description;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.taxonomy.TaxonomicClassification;

import java.util.Locale;
import java.util.Set;

/**
 * Persists a {@link CatalogIdentification} aggregate atomically — ensuring
 * the full Linnaean path (order → family → genus → species) exists before
 * inserting the image and field observation.
 *
 * <p>Idempotent for species: if the species already exists in the catalog,
 * it is left untouched and only the image and observation are inserted.
 * Parent ranks follow the same pattern — existing ranks are never overwritten.
 */
@DomainService
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
        var species = identification.species();

        // 1. Resolve parent ranks (order → family → genus)
        var orderName = resolveOrder(taxonomy);
        var familyName = resolveFamily(taxonomy, orderName);
        resolveGenus(species.genusName(), familyName, taxonomy);

        // 2. Insert species if new
        if (insectQuery.species().getByName(species.name()).isEmpty()) {
            insectCommand.species().insert(species);
        }

        // 3. Insert image and observation
        insectCommand.images().insert(identification.image());
        insectCommand.fieldObservations().insert(identification.observation());
    }

    private InsectOrderName resolveOrder(TaxonomicClassification taxonomy) {
        var orderSlug = taxonomy.order().value().toLowerCase(Locale.ROOT);
        var orderName = InsectOrderName.of(orderSlug);
        if (insectQuery.orders().getByName(orderName).isEmpty()) {
            insectCommand.orders().insert(new InsectOrder(
                    orderName, taxonomy.order(), PLACEHOLDER, Set.of(), null));
        }
        return orderName;
    }

    private InsectFamilyName resolveFamily(TaxonomicClassification taxonomy,
                                           InsectOrderName orderName) {
        var familySlug = taxonomy.family().value().toLowerCase(Locale.ROOT);
        var familyName = InsectFamilyName.of(familySlug);
        if (insectQuery.families().getByName(familyName).isEmpty()) {
            insectCommand.families().insert(new InsectFamily(
                    familyName, orderName, taxonomy.family(), PLACEHOLDER, Set.of(), null));
        }
        return familyName;
    }

    private void resolveGenus(InsectGenusName genusName, InsectFamilyName familyName,
                              TaxonomicClassification taxonomy) {
        if (insectQuery.genera().getByName(genusName).isEmpty()) {
            insectCommand.genera().insert(new InsectGenus(
                    genusName, familyName, taxonomy.genus(), PLACEHOLDER, Set.of(), null));
        }
    }
}
