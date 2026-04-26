package com.naturalist.chemistry.console;

import com.naturalist.chemistry.ChemistryTestContext;
import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.compound.CompoundQuery;
import com.naturalist.data.NaturalistDatabase;
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

    ChemistryController() {
        //TODO:: This will eventually be a spring managed bean
        ChemistryTestContext context = ChemistryTestContext.create(NaturalistDatabase.create());
        this.compoundQuery = context.compoundQuery();
    }

    @GetMapping
    String list(Model model) {
        var nameSet = compoundQuery.allCompoundNames().stream().collect(Collectors.toSet());
        var compounds = compoundQuery.findByNameSet(nameSet).stream()
                .sorted(Comparator.comparing((Compound c) -> c.name().value()))
                .toList();
        model.addAttribute("compounds", compounds);
        return "chemistry/list";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name, Model model) {
        var compoundName = CompoundName.of(name);
        var compound = compoundQuery.getByName(compoundName);
        if (compound.isEmpty()) {
            return "redirect:/chemistry";
        }
        model.addAttribute("compound", compound.get());
        return "chemistry/detail";
    }
}
