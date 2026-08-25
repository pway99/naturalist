package com.naturalist.insects;

import com.naturalist.data.Transaction;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.OrganismFeatureAssignment;
import com.naturalist.taxonomy.TaxonomicClassification;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Persists a {@link CatalogIdentification} aggregate atomically — ensuring
 * the full Linnaean path up to the identified rank exists before inserting
 * the image and field observation.
 *
 * <p>Idempotent for the identified entity: if it already exists in the catalog,
 * it is left untouched and only the image, observation, and features are
 * persisted (the image and observation are always inserted; features are
 * saved — see below). Parent ranks follow the same pattern — existing ranks
 * are never overwritten.
 *
 * <p>Parent-rank resolution is conditional on the identified rank: a family-level
 * identification resolves only the parent order, not the family or genus itself
 * (those are the identified entity's concern). A species-level identification
 * resolves order → family → genus before inserting the species.
 *
 * <p>Features and their rank assignments are persisted via {@code save()}, not
 * {@code insert()}: a repeated exact feature value (e.g. the same taxon
 * re-identified) reuses the existing {@link InsectFeature} row rather than
 * failing the whole transaction on its unique {@code value} constraint. This
 * is exact-string matching only, not semantic deduplication — see the
 * step-4 comment in {@link #doExecute} for what it does and does not cover.
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
        insectCommand.observations().insert(identification.observation());

        // 4. Save features and assignments -- save(), not insert(). A feature
        //    string that already exists in the catalog (very likely on a
        //    re-identification of the same rank -- the same taxon tends to
        //    yield the same field marks) is matched by InsectFeature's "value"
        //    unique constraint and its existing row is reused; insert() would
        //    throw UniqueConstraintException there and fail the whole
        //    transaction. This is a same-string match only -- "dark (black)
        //    pronotum..." and "dark/black pronotum..." are two different
        //    strings and remain two separate rows. That is not dedup, it is
        //    "don't fail on an exact repeat"; semantic near-duplicate merging
        //    is a separate, unsolved problem.
        //
        //    save()'s unique-constraint branch discards the id resolveFeatures()
        //    minted for a matched feature and reuses the existing row's id
        //    instead (see TestEntitySource#save). The assignments built
        //    alongside that feature in resolveFeatures() were built with the
        //    now-discarded id, so they are remapped to the id save() actually
        //    persisted before being saved themselves -- otherwise an assignment
        //    would carry a featureId that was never written (a dangling FK).
        //    Assignments are saved, not inserted, for the same reason as
        //    features: the (featureId, rankName) pair from a re-identification
        //    is likely to already exist once the featureId above is remapped
        //    to the existing feature.
        var resolvedFeatureIds = new HashMap<InsectFeatureId, InsectFeatureId>();
        for (var feature : identification.newFeatures()) {
            var persisted = insectCommand.features().save(feature);
            resolvedFeatureIds.put(feature.id(), persisted.id());
        }
        for (var assignment : identification.featureAssignments()) {
            var resolvedFeatureId = resolvedFeatureIds.getOrDefault(
                    assignment.featureId(), assignment.featureId());
            var toSave = resolvedFeatureId.equals(assignment.featureId())
                    ? assignment
                    : OrganismFeatureAssignment.of(
                            assignment.id(), resolvedFeatureId, assignment.rankName(), assignment.ordinal());
            insectCommand.featureAssignments().save(toSave);
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
