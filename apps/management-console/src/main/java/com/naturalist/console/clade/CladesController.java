package com.naturalist.console.clade;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeCatalog;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class CladesController {

    private final DescriptionRenderer descriptionRenderer = new DescriptionRenderer(List.of());

    @GetMapping("/clades")
    String tree(Model model) {
        model.addAttribute("root", toNode(new Eukaryota()));
        return "clades/tree";
    }

    @GetMapping("/clades/{slug}")
    String detail(@PathVariable String slug, Model model) {
        Clade clade;
        try {
            clade = Clade.of(slug);
        } catch (IllegalArgumentException e) {
            return "redirect:/clades";
        }
        var description = clade.description();
        model.addAttribute("displayName", clade.displayName());
        model.addAttribute("slug", clade.slug());
        model.addAttribute("ancestry", ancestryLinks(clade));
        model.addAttribute("children", childLinks(clade));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "clades/detail";
    }

    private CladeNode toNode(Clade clade) {
        List<CladeNode> children = CladeCatalog.childrenOf(clade).stream()
                .map(this::toNode)
                .toList();
        return new CladeNode(clade.slug(), clade.displayName(), children);
    }

    /** Ancestors from root down to the clade's parent (the clade itself excluded). */
    private List<CladeLink> ancestryLinks(Clade clade) {
        List<Clade> chain = new ArrayList<>(CladeTraversal.ancestry(clade));
        chain.removeFirst();              // drop the clade itself
        Collections.reverse(chain);       // root → parent order
        return chain.stream().map(c -> new CladeLink(c.slug(), c.displayName())).toList();
    }

    private List<CladeLink> childLinks(Clade clade) {
        return CladeCatalog.childrenOf(clade).stream()
                .map(c -> new CladeLink(c.slug(), c.displayName()))
                .toList();
    }

    public record CladeNode(String slug, String displayName, List<CladeNode> children) {
    }

    public record CladeLink(String slug, String displayName) {
    }
}
