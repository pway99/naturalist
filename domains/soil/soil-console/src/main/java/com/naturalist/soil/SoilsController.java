package com.naturalist.soil;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.data.PageRequest;
import com.naturalist.library.GlossaryTermQuery;
import com.naturalist.library.GlossaryLinker;
import com.naturalist.soil.catalog.NutrientChemistryLinks;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Read-only viewer for the Oak Vista soil profiles: a list of profiles and a per-profile detail
 * page showing each dated analysis's nutrient panel and physical characteristics.
 */
@Controller
@RequestMapping("/soil")
public class SoilsController {

    private final SoilProfileInfoQuery soilProfileInfoQuery;
    private final SoilProfileQuery soilProfileQuery;
    private final GlossaryLinker glossaryLinker;
    private final NutrientChemistryLinks chemistryLinks;

    SoilsController(Catalog catalog,
                    EntityRefLinker linker,
                    SoilProfileInfoQuery soilProfileInfoQuery,
                    SoilProfileQuery soilProfileQuery,
                    GlossaryTermQuery glossaryTermQuery) {
        this.soilProfileInfoQuery = soilProfileInfoQuery;
        this.soilProfileQuery = soilProfileQuery;
        // Vocabulary for the inline definition popovers on the profile page. Same glossary the
        // insect Field Marks link against; loaded from the shared in-memory library context.
        this.glossaryLinker = GlossaryLinker.of(glossaryTermQuery
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE))
                .content());
        // Nutrient rows link to the substance each measures, resolved through the
        // cross-domain catalog seam — soil-console never depends on chemistry.
        this.chemistryLinks = NutrientChemistryLinks.of(catalog, linker);
    }

    @GetMapping
    String index() {
        return "redirect:/soil/profiles";
    }

    @GetMapping("/profiles")
    String profiles(Model model) {
        List<SoilProfile> profiles = soilProfileInfoQuery
                .findPage(PageRequest.console(0)).content().stream()
                .map(info -> soilProfileQuery.getBySoilProfileName(info.name()).orElseThrow())
                .toList();
        model.addAttribute("profiles", profiles);
        return "soil/list";
    }

    @GetMapping("/profiles/{name}")
    String profileDetail(@PathVariable String name, Model model) {
        var profile = soilProfileQuery.getBySoilProfileName(SoilProfileName.of(name));
        if (profile.isEmpty()) {
            return "redirect:/soil/profiles";
        }
        model.addAttribute("profile", profile.get());
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("chemistryLinks", chemistryLinks);
        return "soil/profile";
    }
}
