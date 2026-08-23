package com.naturalist.insects;

/**
 * Renderable view-model for one collapsible "ancestor intro" panel on a
 * detail page — the four-level Durrell {@code Description} of a taxonomic
 * ancestor (Class, Order, Family, Genus, Species) of the page's primary
 * entity.
 *
 * <p>{@code label} is the panel heading shown in the collapsed summary
 * (e.g. "Class Insecta", "Order Lepidoptera"). {@code storageKey} is the
 * localStorage key used to persist the open/closed state across pages —
 * use one key per rank type ("intro-class-open", "intro-order-open", …)
 * so opening "Order" on one page keeps "Order" open on sibling pages
 * without coupling rank panels at different levels.
 *
 * <p>The four description strings are pre-rendered HTML; the template
 * emits them via {@code $unsafe{...}}.
 */
public record AncestorIntro(String label, String storageKey,
                            String preschool, String elementary,
                            String secondary, String university) {
}
