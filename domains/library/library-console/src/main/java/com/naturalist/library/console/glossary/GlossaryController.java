package com.naturalist.library.console.glossary;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import com.naturalist.library.GlossaryTerm;
import com.naturalist.library.GlossaryTermName;
import com.naturalist.library.GlossaryTermQuery;
import com.naturalist.library.LibraryTestContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;

@Controller
class GlossaryController {

    private final GlossaryTermQuery glossaryTermQuery;

    GlossaryController() {
        // TODO:: This will eventually be a spring managed bean
        this.glossaryTermQuery = LibraryTestContext.create(NaturalistDatabase.create()).glossaryTermQuery();
    }

    @GetMapping("/glossary")
    String list(Model model) {
        List<GlossaryTerm> terms = glossaryTermQuery
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE))
                .content().stream()
                .sorted(Comparator.comparing(GlossaryTerm::term, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("terms", terms);
        return "glossary/list";
    }

    @GetMapping("/glossary/{slug}")
    String detail(@PathVariable String slug, Model model) {
        var term = glossaryTermQuery.getByName(GlossaryTermName.of(slug));
        if (term.isEmpty()) {
            return "redirect:/glossary";
        }
        model.addAttribute("term", term.get());
        return "glossary/detail";
    }
}
