package com.naturalist.console.search;

import com.naturalist.catalog.*;
import com.naturalist.chemistry.ChemistryDomain;
import com.naturalist.insects.InsectsDomain;
import com.naturalist.plants.PlantsDomain;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController {

    private final Catalog catalog;
    private final EntityRefLinker linker;
    private final Map<DomainId, String> domainDisplayNames;

    SearchController(Catalog catalog, EntityRefLinker linker) {
        this.catalog = catalog;
        this.linker = linker;
        this.domainDisplayNames = Map.of(
                new PlantsDomain(), "Plants",
                new ChemistryDomain(), "Chemistry",
                new InsectsDomain(), "Insects"
        );
    }

    @GetMapping("/search")
    String search(@RequestParam(value = "q", required = false) String q, Model model) {
        String query = q == null ? "" : q.trim();
        SearchResults results = catalog.search(query);
        model.addAttribute("query", query);
        model.addAttribute("groups", buildGroups(results));
        model.addAttribute("totalHits", results.size());
        return "search/results";
    }

    private List<Group> buildGroups(SearchResults results) {
        Map<DomainId, List<Link>> grouped = new LinkedHashMap<>();
        for (SearchHit hit : results.stream().toList()) {
            var ref = hit.target();
            String url = linker.linkFor(ref);
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

    private String displayNameFor(DomainId domain) {
        return domainDisplayNames.getOrDefault(domain, domain.value());
    }

    public record Group(String domainDisplayName, List<Link> links) {
    }

    public record Link(String label, String url, String matchedHint) {
    }
}
