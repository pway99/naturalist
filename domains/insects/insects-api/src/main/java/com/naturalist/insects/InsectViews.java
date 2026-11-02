package com.naturalist.insects;

import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import com.naturalist.observation.OrganismObservation;

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
}
