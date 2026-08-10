package com.naturalist.soil.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.library.console.GlossaryLinker;
import com.naturalist.soil.SoilProfile;
import com.naturalist.soil.SoilProfileInfoQuery;
import com.naturalist.soil.SoilProfileName;
import com.naturalist.soil.SoilProfileQuery;
import com.naturalist.soil.SoilTestContext;
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

    SoilsController() {
        // TODO: becomes a Spring-managed bean when the rdbms adapter replaces the in-memory context.
        NaturalistDatabase db = NaturalistDatabase.create();
        SoilTestContext context = SoilTestContext.create(db);
        this.soilProfileInfoQuery = context.soilProfileInfoQuery();
        this.soilProfileQuery = context.soilProfileQuery();
        // Vocabulary for the inline definition popovers on the profile page. Same glossary the
        // insect Field Marks link against; loaded from the shared in-memory library context.
        this.glossaryLinker = GlossaryLinker.of(LibraryTestContext.create(db).glossaryTermQuery()
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE))
                .content());
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
        return "soil/profile";
    }
}
