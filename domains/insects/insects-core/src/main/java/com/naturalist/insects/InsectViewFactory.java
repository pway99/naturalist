package com.naturalist.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Assembles {@link InsectViews} presentation models from the biology {@link InsectQuery} serves.
 * Package-private: only {@link InsectViewQueryImpl} (and its tests) construct one.
 */
class InsectViewFactory {
    private final Observer observer = Observer.forClass(getClass());

    private final InsectQuery insectQuery;

    InsectViewFactory(InsectQuery insectQuery) {
        observer.arguments("constructor", i -> i.notNull(insectQuery, "insectQuery")).throwWhenInvalid();
        this.insectQuery = insectQuery;
    }

    Optional<InsectViews.RankPage> buildRankPage(InsectRankName name, @Nullable NaturalistName viewer) {
        observer.arguments("buildRankPage", i -> i.identifier(name, "name").whenNotNull(viewer, v -> v.entityName(viewer, "viewer"))).throwWhenInvalid();
        Optional<Insect> found = insectQuery.getByName(name);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Insect insect = found.get();
        if (viewer == null) {
            return Optional.of(InsectViews.RankPage.anonymous(insect));
        }
        InsectEntityCollections.ObservationCollection mine =
                insectQuery.observations().forNaturalistAndSubjects(viewer, Set.<InsectRankName>of(name));
        Map<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>> mineById = mine.stream()
                .collect(Collectors.toMap(OrganismObservation::id, Function.identity()));
        List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> ownImages = new ArrayList<>();
        Map<InsectImageId, OrganismObservation<InsectObservationId, InsectRankName>> ownObservationByImage = new LinkedHashMap<>();
        for (OrganismImage<InsectImageId, InsectObservationId, InsectRankName> image : insect.observations().stream().toList()) {
            OrganismObservation<InsectObservationId, InsectRankName> observation =
                    image.observationId() == null ? null : mineById.get(image.observationId());
            if (observation != null) {
                ownImages.add(image);
                ownObservationByImage.put(image.id(), observation);
            }
        }
        return Optional.of(new InsectViews.RankPage(insect, new InsectViews.Viewer(
                !mineById.isEmpty(), InsectEntityCollections.ImageCollection.of(ownImages), ownObservationByImage)));
    }
}
