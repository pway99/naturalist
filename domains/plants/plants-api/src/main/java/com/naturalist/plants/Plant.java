package com.naturalist.plants;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import com.naturalist.plants.cultivar.CultivarCollection;
import com.naturalist.plants.management.PlantProgramCollection;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The Plant read model — in-memory composition of everything known about a plant at
 * whatever identification depth was reached. Chunk 1 carries only the rank chain
 * ({@code @Nullable} {@link PlantOrderView}/{@link PlantFamilyView}/{@link PlantGenusView}/
 * {@link PlantSpeciesView}), populated top-down to the resolved depth. Features, children,
 * role, images, and species extras fold in over later chunks (see the design of record).
 * Mirrors {@code Insect}.
 *
 * <p>Construction never throws; invalid states are reported by {@link #invariants()} when a
 * consumer asks an {@link com.naturalist.observability.Observer} to walk them.
 */
public record Plant(
        @Nullable PlantOrderView order,
        @Nullable PlantFamilyView family,
        @Nullable PlantGenusView genus,
        @Nullable PlantSpeciesView species,
        @Nullable PlantFeatureView features,
        /**
         * Direct sub-taxa of the identified rank, each wrapped as its rank's {@link PlantTaxonView}
         * permit with EMPTY {@code images()} — plants render no child thumbnails (a future
         * gallery-bearing variant would be a separate slice). Never null; empty for species (the
         * bottom rank) and for {@link #empty()}.
         */
        List<PlantTaxonView> children,
        @Nullable PlantEcologicalRole role,
        ImageCollection images,
        /**
         * Cultivars bred from this taxon. Species-only — non-empty only when {@link #species}
         * is set; every other rank carries {@link CultivarCollection#empty()}.
         */
        CultivarCollection cultivars,
        /**
         * Management programs targeting this taxon. Species-only in current composition —
         * see {@code PlantFactory}; the underlying query attaches at any rank.
         */
        PlantProgramCollection programs,
        /**
         * Phytochemical constituents recorded for this taxon. Species-only in current
         * composition — see {@code PlantFactory}; the underlying query attaches at any rank.
         */
        PhytochemicalConstituentCollection constituents
) implements ReadModel {

    /** Zero-state read model — no rank identified. Starting point for {@code with*} refinement. */
    public static Plant empty() {
        return new Plant(null, null, null, null, null, List.of(), null, ImageCollection.empty(),
                CultivarCollection.empty(), PlantProgramCollection.empty(), PhytochemicalConstituentCollection.empty());
    }

    /** Most-specific identified rank's typed name, if any. */
    public Optional<PlantRankName> identifiedTo() {
        if (species != null) return Optional.of(species.name());
        if (genus != null) return Optional.of(genus.name());
        if (family != null) return Optional.of(family.name());
        if (order != null) return Optional.of(order.name());
        return Optional.empty();
    }

    public Optional<PlantOrderName> orderName() {
        return order == null ? Optional.empty() : Optional.of(order.name());
    }

    public Optional<PlantFamilyName> familyName() {
        return family == null ? Optional.empty() : Optional.of(family.name());
    }

    public Optional<PlantGenusName> genusName() {
        return genus == null ? Optional.empty() : Optional.of(genus.name());
    }

    public Optional<PlantSpeciesName> speciesName() {
        return species == null ? Optional.empty() : Optional.of(species.name());
    }

    public Plant withOrder(@Nullable PlantOrderView order) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withFamily(@Nullable PlantFamilyView family) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withGenus(@Nullable PlantGenusView genus) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withSpecies(@Nullable PlantSpeciesView species) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withFeatures(@Nullable PlantFeatureView features) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withChildren(List<PlantTaxonView> children) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withRole(@Nullable PlantEcologicalRole role) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withImages(ImageCollection images) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withCultivars(CultivarCollection cultivars) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withPrograms(PlantProgramCollection programs) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    public Plant withConstituents(PhytochemicalConstituentCollection constituents) {
        return new Plant(order, family, genus, species, features, children, role, images,
                cultivars, programs, constituents);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .whenNotNull(order, o -> o.readModel(order, "order"))
                .whenNotNull(family, f -> f
                        .readModel(family, "family")
                        .notNull(order, "family:order")
                        .isTrue(family.belongsToOrder(order), "familyBelongsToOrder"))
                .whenNotNull(genus, g -> g
                        .readModel(genus, "genus")
                        .notNull(family, "genus:family")
                        .isTrue(genus.belongsToFamily(family), "genusBelongsToFamily"))
                .whenNotNull(species, s -> s
                        .readModel(species, "species")
                        .notNull(genus, "species:genus")
                        .isTrue(species.belongsToGenus(genus), "speciesBelongsToGenus"))
                .whenNotNull(features, f -> f.readModel(features, "features"))
                .notNull(children, "children")
                .namedEntityOrNull(role, "role")
                .behavioralCollection(images, "images")
                .behavioralCollection(cultivars, "cultivars")
                .behavioralCollection(programs, "programs")
                .behavioralCollection(constituents, "constituents");
    }
}
