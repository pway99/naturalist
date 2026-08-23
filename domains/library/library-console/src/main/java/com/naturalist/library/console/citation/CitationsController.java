package com.naturalist.library.console.citation;

import com.naturalist.authority.Citation;
import com.naturalist.data.PageRequest;
import com.naturalist.library.CitationQuery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
class CitationsController {

    private final CitationQuery citationQuery;

    CitationsController(CitationQuery citationQuery) {
        this.citationQuery = citationQuery;
    }

    @GetMapping("/citations")
    String list(Model model) {
        List<Citation> citations = citationQuery
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE))
                .content().stream()
                .sorted(Comparator.comparing(Citation::title))
                .toList();
        model.addAttribute("citations", citations);
        return "citations/list";
    }
}
