package com.naturalist.chemistry.console;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.chemistry.ChemistryTestContext;
import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundDepiction;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.compound.CompoundQuery;
import com.naturalist.chemistry.console.catalog.BackReferencesViewModel;
import com.naturalist.chemistry.element.Element;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.chemistry.element.ElementQuery;
import com.naturalist.chemistry.product.Product;
import com.naturalist.chemistry.product.ProductName;
import com.naturalist.chemistry.product.ProductQuery;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/chemistry")
public class ChemistryController {

    private final CompoundQuery compoundQuery;
    private final ProductQuery productQuery;
    private final ElementQuery elementQuery;
    private final DepictionRenderer depictionRenderer;
    private final Catalog catalog;
    private final EntityRefLinker linker;

    ChemistryController(DepictionRenderer depictionRenderer, Catalog catalog, EntityRefLinker linker) {
        //TODO:: This will eventually be a spring managed bean
        ChemistryTestContext context = ChemistryTestContext.create(NaturalistDatabase.create());
        this.compoundQuery = context.compoundQuery();
        this.productQuery = context.productQuery();
        this.elementQuery = context.elementQuery();
        this.depictionRenderer = depictionRenderer;
        this.catalog = catalog;
        this.linker = linker;
    }

    @GetMapping
    String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Compound> compoundsPage = compoundQuery.compounds()
                .findPage(PageRequest.console(Math.max(0, page)));
        var depictableSlugs = compoundQuery.depictions().allDepictedCompounds().stream()
                .map(CompoundName::value)
                .collect(Collectors.toSet());
        model.addAttribute("compoundsPage", compoundsPage);
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
        var backReferences = BackReferencesViewModel.from(catalog.findReferencesTo(compoundName), linker);
        var linkableElements = elementQuery
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE)).content().stream()
                .map(e -> e.name().value())
                .collect(Collectors.toSet());
        model.addAttribute("compound", compound.get());
        model.addAttribute("hasDepiction", depiction.isPresent());
        model.addAttribute("depictionNote", depiction.map(CompoundDepiction::note).orElse(null));
        model.addAttribute("backReferences", backReferences);
        model.addAttribute("linkableElements", linkableElements);
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

    @GetMapping("/elements")
    String elementList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Element> elementsPage = elementQuery.findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("elementsPage", elementsPage);
        return "chemistry/elements/list";
    }

    @GetMapping("/elements/{name}")
    String elementDetail(@PathVariable String name, Model model) {
        var element = elementQuery.getByName(ElementName.of(name));
        if (element.isEmpty()) {
            return "redirect:/chemistry/elements";
        }
        model.addAttribute("element", element.get());
        return "chemistry/elements/detail";
    }

    @GetMapping("/products")
    String productList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Product> productsPage = productQuery.findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("productsPage", productsPage);
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
