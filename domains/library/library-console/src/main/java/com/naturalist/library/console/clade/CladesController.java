package com.naturalist.library.console.clade;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.library.CladeQuery;
import com.naturalist.library.CladeView;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Controller
public class CladesController {

    private final CladeQuery cladeQuery;
    private final Catalog catalog;
    private final EntityRefLinker linker;
    private final DescriptionRenderer descriptionRenderer = new DescriptionRenderer(List.of());

    CladesController(Catalog catalog, EntityRefLinker linker, CladeQuery cladeQuery) {
        this.cladeQuery = cladeQuery;
        this.catalog = catalog;
        this.linker = linker;
    }

    @GetMapping("/clades")
    String tree(Model model) {
        model.addAttribute("root", cladeQuery.tree());
        var eukaryota = cladeQuery.getBySlug("eukaryota");
        eukaryota.ifPresent(view -> model.addAttribute("rootView", view));
        return "clades/tree";
    }

    @GetMapping("/clades/{slug}")
    String detail(@PathVariable String slug, Model model) {
        // Insecta is the single class owned by the insects catalog; its
        // tree-of-life page would list only the few insect orders modelled as
        // clades, so send the naturalist to the full catalog instead.
        if ("insecta".equals(slug)) {
            return "redirect:/insects/orders";
        }
        var view = cladeQuery.getBySlug(slug);
        if (view.isEmpty()) {
            return "redirect:/clades";
        }
        CladeView v = view.get();
        var description = findCladeDescription(slug);
        model.addAttribute("view", v);
        model.addAttribute("rankLinks", rankLinks(v));
        model.addAttribute("descriptionPreschool", description[0]);
        model.addAttribute("descriptionElementary", description[1]);
        model.addAttribute("descriptionSecondary", description[2]);
        model.addAttribute("descriptionUniversity", description[3]);
        return "clades/detail";
    }

    /**
     * Clade slug → rank-eyebrow URL for the subject and every ranked ancestor.
     * The eyebrow bridges into the Linnaean catalog when a taxon exists there,
     * else falls back to the rank-definition concept — see {@link CladeRankLinks}.
     */
    private Map<String, String> rankLinks(CladeView v) {
        Map<String, String> links = new HashMap<>();
        Stream.concat(v.ancestry().stream(), Stream.of(v.subject()))
                .filter(step -> step.rank().isPresent())
                .forEach(step -> links.put(step.cladeSlug(),
                        CladeRankLinks.forStep(catalog, linker, step.cladeSlug(), step.rank().get())));
        return links;
    }

    /**
     * Resolves the four-level description for a clade by its slug.
     * Clade descriptions live on the Clade sealed permits in the clades kernel,
     * accessed indirectly through the kernel's static API.
     */
    private String[] findCladeDescription(String slug) {
        try {
            var clade = com.naturalist.clades.Clade.of(slug);
            var description = clade.description();
            return new String[]{
                    descriptionRenderer.render(description.preschool()),
                    descriptionRenderer.render(description.elementary()),
                    descriptionRenderer.render(description.secondary()),
                    descriptionRenderer.render(description.university())
            };
        } catch (IllegalArgumentException e) {
            return new String[]{"", "", "", ""};
        }
    }
}
