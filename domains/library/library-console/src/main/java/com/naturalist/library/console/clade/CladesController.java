package com.naturalist.library.console.clade;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.library.CladeQuery;
import com.naturalist.library.CladeView;
import com.naturalist.library.LibraryTestContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class CladesController {

    private final CladeQuery cladeQuery;
    private final DescriptionRenderer descriptionRenderer = new DescriptionRenderer(List.of());

    CladesController() {
        // TODO:: This will eventually be a spring managed bean
        this.cladeQuery = LibraryTestContext.create(NaturalistDatabase.create()).cladeQuery();
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
        model.addAttribute("descriptionPreschool", description[0]);
        model.addAttribute("descriptionElementary", description[1]);
        model.addAttribute("descriptionSecondary", description[2]);
        model.addAttribute("descriptionUniversity", description[3]);
        return "clades/detail";
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
