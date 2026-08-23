package com.naturalist.library.concept;

import com.naturalist.data.PageRequest;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.library.Concept;
import com.naturalist.library.ConceptName;
import com.naturalist.library.ConceptQuery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;

@Controller
class ConceptsController {

    private final ConceptQuery conceptQuery;
    private final DescriptionRenderer descriptionRenderer = new DescriptionRenderer(List.of());

    ConceptsController(ConceptQuery conceptQuery) {
        this.conceptQuery = conceptQuery;
    }

    @GetMapping("/concepts")
    String list(Model model) {
        List<Concept> concepts = conceptQuery
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE))
                .content().stream()
                .sorted(Comparator.comparing(Concept::title))
                .toList();
        model.addAttribute("concepts", concepts);
        return "concepts/list";
    }

    @GetMapping("/concepts/{slug}")
    String detail(@PathVariable String slug, Model model) {
        var concept = conceptQuery.getByName(ConceptName.of(slug));
        if (concept.isEmpty()) {
            return "redirect:/concepts";
        }
        Concept c = concept.get();
        var description = c.description();
        model.addAttribute("concept", c);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "concepts/detail";
    }
}
