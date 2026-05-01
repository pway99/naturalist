package com.naturalist.chemistry.console;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.chemistry.ChemistryTestContext;
import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.compound.CompoundQuery;
import com.naturalist.chemistry.console.catalog.BackReferencesViewModel;
import com.naturalist.chemistry.product.Product;
import com.naturalist.chemistry.product.ProductName;
import com.naturalist.chemistry.product.ProductQuery;
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
    private final ProductQuery productQuery;
    private final DepictionRenderer depictionRenderer;
    private final Catalog catalog;
    private final EntityRefLinker linker;

    ChemistryController(DepictionRenderer depictionRenderer, Catalog catalog, EntityRefLinker linker) {
        //TODO:: This will eventually be a spring managed bean
        ChemistryTestContext context = ChemistryTestContext.create(NaturalistDatabase.create());
        this.compoundQuery = context.compoundQuery();
        this.productQuery = context.productQuery();
        this.depictionRenderer = depictionRenderer;
        this.catalog = catalog;
        this.linker = linker;
    }

    @GetMapping
    String list(Model model) {
        var nameSet = compoundQuery.compounds().allCompoundNames().stream().collect(Collectors.toSet());
        var compounds = compoundQuery.compounds().findByNameSet(nameSet).stream()
                .sorted(Comparator.comparing((Compound c) -> c.name().value()))
                .toList();
        var depictableSlugs = compoundQuery.depictions().allDepictedCompounds().stream()
                .map(CompoundName::value)
                .collect(Collectors.toSet());
        model.addAttribute("compounds", compounds);
        model.addAttribute("depictableSlugs", depictableSlugs);
        return "chemistry/list";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name, Model model) {
        var compoundName = CompoundName.of(name);
        var compound = compoundQuery.compounds().getByName(compoundName);
        if (compound.isEmpty()) {
            return "redirect:/chemistry";
        }
        var depiction = compoundQuery.depictions().getByCompoundName(compoundName);
        var products = productQuery.findByCompoundName(compoundName).stream()
                .sorted(Comparator.comparing((Product p) -> p.name().value()))
                .toList();
        var backReferences = BackReferencesViewModel.from(catalog.findReferencesTo(compoundName), linker);
        model.addAttribute("compound", compound.get());
        model.addAttribute("hasDepiction", depiction.isPresent());
        model.addAttribute("depictionNote", depiction.map(d -> d.note()).orElse(null));
        model.addAttribute("products", products);
        model.addAttribute("backReferences", backReferences);
        return "chemistry/detail";
    }

    @GetMapping(value = "/{name}/depiction.svg", produces = "image/svg+xml")
    ResponseEntity<String> depiction(@PathVariable String name) {
        return compoundQuery.depictions().getByCompoundName(CompoundName.of(name))
                .map(depictionRenderer::renderSvg)
                .map(svg -> ResponseEntity.ok()
                        .contentType(MediaType.valueOf("image/svg+xml"))
                        .body(svg))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/products")
    String productList(Model model) {
        var nameSet = productQuery.allProductNames().stream().collect(Collectors.toSet());
        var products = productQuery.findByNameSet(nameSet).stream()
                .sorted(Comparator.comparing((Product p) -> p.name().value()))
                .toList();
        model.addAttribute("products", products);
        return "chemistry/products/list";
    }

    @GetMapping("/products/{name}")
    String productDetail(@PathVariable String name, Model model) {
        var productName = ProductName.of(name);
        var product = productQuery.getByName(productName);
        if (product.isEmpty()) {
            return "redirect:/chemistry/products";
        }
        var compounds = compoundQuery.compounds().findByNameSet(product.get().compounds()).stream()
                .sorted(Comparator.comparing((Compound c) -> c.name().value()))
                .toList();
        model.addAttribute("product", product.get());
        model.addAttribute("compounds", compounds);
        return "chemistry/products/detail";
    }
}
