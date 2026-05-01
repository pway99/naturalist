package com.naturalist.console.search;

import com.naturalist.atlas.Atlas;
import com.naturalist.atlas.DomainId;
import com.naturalist.atlas.SearchHit;
import com.naturalist.atlas.SearchResults;
import com.naturalist.atlas.inmem.AtlasAssembly;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.PlantsTestContext;
import com.naturalist.plants.atlas.PlantAtlasContribution;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Controller
public class SearchController {

    private final Atlas atlas;
    private final Map<Class<? extends EntityName>, Function<EntityName, String>> urlBuilders;
    private final Map<DomainId, String> domainDisplayNames;

    SearchController() {
        //TODO:: This will eventually be a spring managed bean
        var plantContext = PlantsTestContext.create(NaturalistDatabase.create());
        this.atlas = AtlasAssembly.from(new PlantAtlasContribution(plantContext.plantQuery().plants()));
        this.urlBuilders = Map.of(
                PlantName.class, name -> "/plants/" + name.value(),
                CompoundName.class, name -> "/chemistry/" + name.value(),
                InsectSpeciesName.class, name -> "/insects/" + name.value()
        );
        this.domainDisplayNames = Map.of(
                new DomainId.Plants(), "Plants",
                new DomainId.Chemistry(), "Chemistry",
                new DomainId.Insects(), "Insects"
        );
    }

    @GetMapping("/search")
    String search(@RequestParam(value = "q", required = false) String q, Model model) {
        String query = q == null ? "" : q.trim();
        SearchResults results = atlas.search(query);
        model.addAttribute("query", query);
        model.addAttribute("groups", buildGroups(results));
        model.addAttribute("totalHits", results.size());
        return "search/results";
    }

    private List<Group> buildGroups(SearchResults results) {
        Map<DomainId, List<Link>> grouped = new LinkedHashMap<>();
        for (SearchHit hit : results.stream().toList()) {
            var ref = hit.target();
            String url = urlFor(ref.name());
            if (url == null) {
                continue;
            }
            String slug = ref.name().value();
            String matchedHint = hit.matchedToken().equalsIgnoreCase(slug) ? null : hit.matchedToken();
            grouped.computeIfAbsent(ref.domain(), d -> new ArrayList<>())
                    .add(new Link(slug, url, matchedHint));
        }
        List<Group> groups = new ArrayList<>(grouped.size());
        grouped.forEach((domain, links) ->
                groups.add(new Group(displayNameFor(domain), List.copyOf(links))));
        return List.copyOf(groups);
    }

    private String urlFor(EntityName name) {
        Function<EntityName, String> builder = urlBuilders.get(name.getClass());
        return builder == null ? null : builder.apply(name);
    }

    private String displayNameFor(DomainId domain) {
        return domainDisplayNames.getOrDefault(domain, domain.value());
    }

    public record Group(String domainDisplayName, List<Link> links) {}

    public record Link(String label, String url, String matchedHint) {}
}
