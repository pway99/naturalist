package com.naturalist.insects.console;

import com.naturalist.insects.InsectFeatureView;
import com.naturalist.insects.InsectRankName;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Display grouping of an {@link InsectFeatureView} — the lineage-composite
 * field marks split into one group per contributing rank, so the page can
 * render "Order-level marks: … / Family-level marks: …".
 *
 * <p>Group order follows the view's ancestor-first contract; within a group,
 * marks are ordered conspicuous-to-diagnostic by
 * {@link InsectFeatureView.RankedFeature#ordinal()}.
 *
 * @param rankLabel display label for the contributing rank ("Order", "Family")
 * @param rankSlug  the contributing rank's slug, for a link back to its page
 * @param marks     the feature values, conspicuous to diagnostic
 */
public record FeatureGroup(String rankLabel, String rankSlug, List<String> marks) {

    /**
     * Groups a feature view for display. Returns an empty list for a null or
     * feature-less view so the template can test one condition.
     */
    public static List<FeatureGroup> of(@Nullable InsectFeatureView view) {
        if (view == null || view.features().isEmpty()) {
            return List.of();
        }
        Map<InsectRankName, List<InsectFeatureView.RankedFeature>> byRank =
                new LinkedHashMap<>();
        for (var ranked : view.features()) {
            byRank.computeIfAbsent(ranked.assignedAt(), k -> new ArrayList<>()).add(ranked);
        }
        List<FeatureGroup> groups = new ArrayList<>(byRank.size());
        byRank.forEach((rankName, ranked) -> {
            List<String> marks = ranked.stream()
                    .sorted(Comparator.comparingInt(InsectFeatureView.RankedFeature::ordinal))
                    .map(r -> r.feature().value())
                    .toList();
            groups.add(new FeatureGroup(label(rankName), rankName.value(), marks));
        });
        return List.copyOf(groups);
    }

    private static String label(InsectRankName rankName) {
        String name = rankName.rank().name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
