package com.naturalist.chemistry.console;

import com.naturalist.chemistry.ChemistryTestContext;
import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.compound.CompoundQuery;
import com.naturalist.chemistry.compound.depiction.DepictionQuery;
import com.naturalist.data.NaturalistDatabase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/chemistry")
public class ChemistryController {

    private final CompoundQuery compoundQuery;
    private final DepictionQuery depictionQuery;
    private final DepictionRenderer depictionRenderer;

    ChemistryController(DepictionRenderer depictionRenderer) {
        //TODO:: This will eventually be a spring managed bean
        ChemistryTestContext context = ChemistryTestContext.create(NaturalistDatabase.create());
        this.compoundQuery = context.compoundQuery();
        this.depictionQuery = context.depictionQuery();
        this.depictionRenderer = depictionRenderer;
    }

    @GetMapping
    String list(Model model) {
        var nameSet = compoundQuery.allCompoundNames().stream().collect(Collectors.toSet());
        var compounds = compoundQuery.findByNameSet(nameSet).stream()
                .sorted(Comparator.comparing((Compound c) -> c.name().value()))
                .toList();
        var depictableSlugs = depictionQuery.allDepictedCompounds().stream()
                .map(CompoundName::value)
                .collect(Collectors.toSet());
        model.addAttribute("compounds", compounds);
        model.addAttribute("depictableSlugs", depictableSlugs);
        return "chemistry/list";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name, Model model) {
        var compoundName = CompoundName.of(name);
        var compound = compoundQuery.getByName(compoundName);
        if (compound.isEmpty()) {
            return "redirect:/chemistry";
        }
        var depiction = depictionQuery.getByCompoundName(compoundName);
        model.addAttribute("compound", compound.get());
        model.addAttribute("hasDepiction", depiction.isPresent());
        model.addAttribute("depictionNote", depiction.map(d -> d.note()).orElse(null));
        return "chemistry/detail";
    }

    @GetMapping(value = "/{name}/depiction.svg", produces = "image/svg+xml")
    ResponseEntity<String> depiction(@PathVariable String name) {
        return depictionQuery.getByCompoundName(CompoundName.of(name))
                .map(depictionRenderer::renderSvg)
                .map(svg -> ResponseEntity.ok()
                        .contentType(MediaType.valueOf("image/svg+xml"))
                        .body(svg))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
