package com.naturalist.chemistry.console.catalog;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renderer-friendly projection of {@code Catalog.findReferencesTo(target)} —
 * the inverse-direction "Found in:" panel on a chemistry detail page (M8).
 * <p>
 * Refs are grouped by {@link DomainId} in the order the catalog returned
 * them; {@link EntityRef}s the supplied {@link EntityRefLinker} cannot
 * resolve to a URL are dropped from the visible output rather than
 * rendered as dead anchors.
 */
public record BackReferencesViewModel(List<Group> groups) {

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    public static BackReferencesViewModel from(Map<DomainId, List<EntityRef>> grouped, EntityRefLinker linker) {
        List<Group> built = new ArrayList<>(grouped.size());
        for (var entry : grouped.entrySet()) {
            List<Link> links = new ArrayList<>();
            for (EntityRef ref : entry.getValue()) {
                String url = linker.linkFor(ref);
                if (url == null) {
                    continue;
                }
                links.add(new Link(ref.name().value(), url));
            }
            if (!links.isEmpty()) {
                built.add(new Group(displayNameFor(entry.getKey()), List.copyOf(links)));
            }
        }
        return new BackReferencesViewModel(List.copyOf(built));
    }

    private static String displayNameFor(DomainId domain) {
        return DISPLAY_NAMES.getOrDefault(domain.value(), domain.value());
    }

    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            "plants", "Plants",
            "insects", "Insects",
            "chemistry", "Chemistry"
    );

    public record Group(String domainDisplayName, List<Link> links) {
    }

    public record Link(String label, String url) {
    }
}
