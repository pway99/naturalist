package com.naturalist.chemistry.element;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the element catalog carries every element a soil nutrient row references.
 * Data assertions, not framework assertions: a missing element silently degrades a
 * nutrient link to plain text, which no other test would notice.
 */
class ElementCatalogDataTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    private static final Map<String, String> NUTRIENT_ELEMENT_SYMBOLS = Map.of(
            "calcium", "Ca",
            "magnesium", "Mg",
            "potassium", "K",
            "nitrogen", "N",
            "sulfur", "S",
            "phosphorus", "P",
            "chlorine", "Cl",
            "sodium", "Na",
            "zinc", "Zn",
            "manganese", "Mn");

    private static final Map<String, String> REMAINING_SYMBOLS = Map.of(
            "iron", "Fe",
            "copper", "Cu",
            "boron", "B");

    private ElementTestEntitySource source() {
        return nte.getNamed(ElementTestEntitySource.class);
    }

    @Test
    void everyNutrientElementIsCatalogued() {
        var slugs = source().entityStream().map(e -> e.name().value()).toList();
        assertThat(slugs).containsAll(NUTRIENT_ELEMENT_SYMBOLS.keySet());
        assertThat(slugs).containsAll(REMAINING_SYMBOLS.keySet());
    }

    @Test
    void eachNutrientElementCarriesItsIupacSymbol() {
        var bySlug = source().entityStream()
                .collect(java.util.stream.Collectors.toMap(e -> e.name().value(), Element::symbol));
        NUTRIENT_ELEMENT_SYMBOLS.forEach((slug, symbol) ->
                assertThat(bySlug).containsEntry(slug, symbol));
        REMAINING_SYMBOLS.forEach((slug, symbol) ->
                assertThat(bySlug).containsEntry(slug, symbol));
    }

    @Test
    void everyElementNameIsAValidSlug() {
        source().entityStream().forEach(e ->
                assertThat(e.name().isValid())
                        .as("element slug '%s' must be lower-kebab-case", e.name().value())
                        .isTrue());
    }

    @Test
    void boronIsNeitherCationNorAnion() {
        // Boron is taken up as undissociated boric acid at Oak Vista's pH 7.2, so
        // its charge is 0 — an honest value, not a missing one.
        Element boron = source().entityStream()
                .filter(e -> e.name().value().equals("boron"))
                .findFirst().orElseThrow();
        assertThat(boron.ionicCharge()).isZero();
        assertThat(boron.isCation()).isFalse();
        assertThat(boron.isAnion()).isFalse();
        assertThat(boron.ionicForm()).isEqualTo("H3BO3");
    }

    @Test
    void micronutrientCationsAreDivalent() {
        var byName = source().entityStream()
                .collect(java.util.stream.Collectors.toMap(e -> e.name().value(), e -> e));
        for (String slug : java.util.List.of("zinc", "manganese", "iron", "copper")) {
            assertThat(byName.get(slug).isDivalent())
                    .as("%s is reported as a divalent cation", slug)
                    .isTrue();
        }
    }
}
