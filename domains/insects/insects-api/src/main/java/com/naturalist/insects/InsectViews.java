package com.naturalist.insects;

import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import com.naturalist.observation.OrganismObservation;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Presentation read models: what a page or list card shows a particular viewer, as opposed to
 * what insects are. Assembled from the biology by {@link InsectViewQuery}; nothing in the
 * biology refers to them.
 */
public interface InsectViews {
    /** What the page knows about the naturalist looking at it; {@link #anonymous()} when no one is signed in. */
    record Viewer(boolean collected, InsectEntityCollections.ImageCollection ownImages, Map<InsectImageId, OrganismObservation<InsectObservationId, InsectRankName>> ownObservationByImage) implements ValueObject {
        public Viewer {
            // Unmodifiable but insertion-ordered; null is left for the invariant to report.
            ownObservationByImage = ownObservationByImage == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(ownObservationByImage));
        }

        public static Viewer anonymous() {
            return new Viewer(false, InsectEntityCollections.ImageCollection.empty(), Map.of());
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notNull(ownImages, "ownImages").notNull(ownObservationByImage, "ownObservationByImage").whenNotNull(ownImages, a -> a.whenNotNull(ownObservationByImage, b -> b.isTrue(keysAreOwnImages(), "observationKeysAreOwnImages")));
        }

        private boolean keysAreOwnImages() {
            return ownObservationByImage.keySet().stream().allMatch(id -> ownImages.stream().anyMatch(image -> image.id().equals(id)));
        }
    }

    /** One rank page as a given naturalist sees it: the biology plus what that naturalist has collected of it. */
    record RankPage(Insect insect, Viewer viewer) implements ReadModel {
        public static RankPage anonymous(Insect insect) {
            return new RankPage(insect, Viewer.anonymous());
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.readModel(insect, "insect").valueObject(viewer, "viewer");
        }
    }

    /** One species-list card: the species with the labels, role, photos and viewer state the card shows. */
    record SpeciesCard(InsectSpeciesInfo species, @Nullable InsectGenusInfo genus, @Nullable InsectFamilyInfo family,
                       @Nullable InsectFunctionalRole role, InsectEntityCollections.ImageCollection images,
                       boolean collected) implements ReadModel {
        public String displayName() {
            return species.displayName(genus);
        }

        public String binomial() {
            return species.binomial(genus);
        }

        public String familyLabel() {
            if (family != null) {
                return family.family().value();
            }
            return genus != null ? genus.familyName().value() : "\u2014";
        }

        public String genusLabel() {
            return genus != null ? genus.genus().value() : species.genusName().value();
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.namedEntity(species, "species").notNull(images, "images")
                    .whenNotNull(genus, g -> g.namedEntity(genus, "genus"))
                    .whenNotNull(family, f -> f.namedEntity(family, "family"))
                    .whenNotNull(role, r -> r.observable(role, "role"));
        }
    }

    /** One rank-list card (order / family / genus): names, labels and the rank's subtree gallery. */
    record RankCard(InsectRankName name, String displayName, String scientificName,
                    @Nullable InsectRankName parentName, @Nullable String parentLabel,
                    InsectEntityCollections.ImageCollection images) implements ReadModel {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.identifier(name, "name").notBlank(displayName, "displayName")
                    .notBlank(scientificName, "scientificName").notNull(images, "images");
        }
    }
}
