package com.naturalist.plants;

/**
 * The ecological and horticultural role a {@link Plant} plays at Oak Vista.
 * <p>
 * A plant commonly fills multiple roles simultaneously — dill is both a
 * {@link #BENEFICIAL_INSECT_HABITAT} (nectar for tachinid flies and braconid wasps)
 * and an {@link #INSECT_LARVAL_HOST} (swallowtail caterpillars). The role set on
 * {@link Plant} captures all confirmed functions at Oak Vista.
 * <p>
 * Roles govern which application module contexts a plant participates in:
 * a {@link #KEYSTONE_HOST} plant triggers zero-pesticide constraints in the
 * PestManagement module; a {@link #NITROGEN_FIXER} feeds into soil nitrogen
 * cycling models; a {@link #FOOD_CROP} is tracked in harvest calendars.
 */
public enum PlantRole {

    /**
     * Produces edible fruit, seed, or vegetative material as primary garden output.
     * Tomatoes, Passiflora edulis, stone fruit trees, citrus.
     */
    FOOD_CROP,

    /**
     * Provides nectar and/or pollen accessible to bees and other beneficial insects.
     * The primary forage plants sustaining the Oak Vista pollinator community.
     */
    POLLINATOR_SUPPORT,

    /**
     * Flower structure or plant chemistry specifically supports parasitoid insects
     * (tachinid flies, braconid wasps) or other predatory beneficials. Apiaceae
     * (dill, fennel) and Lobularia (alyssum) are the prime examples — their short
     * open florets match the short mouthparts of parasitoid flies and wasps.
     */
    BENEFICIAL_INSECT_HABITAT,

    /**
     * Fixes atmospheric nitrogen via root nodule symbiosis with Rhizobium bacteria.
     * Legumes (clovers) are the primary nitrogen fixers at Oak Vista, contributing
     * measurable N to the soil food web without fertiliser input.
     */
    NITROGEN_FIXER,

    /**
     * Planted primarily to cover and protect the soil surface — reducing erosion,
     * suppressing weeds, retaining moisture, and building organic matter.
     * Clover carpet, creeping thyme, and chickweed fill this role at Oak Vista.
     */
    COVER_CROP,

    /**
     * Obligate larval host for a keystone insect species. The plant's presence
     * is non-negotiable for the associated species' breeding population.
     * {@code california-pipevine} is the exclusive larval host for Pipevine
     * Swallowtail ({@code Battus philenor}); ornamental Passiflora hosts the
     * Gulf Fritillary ({@code Agraulis vanillae}).
     * <p>
     * CRITICAL: keystone host plants must never receive pesticide treatment —
     * organic or conventional — while larvae are present.
     */
    KEYSTONE_HOST,

    /**
     * Larval food plant for butterfly or moth caterpillars that are not classified
     * as keystone species at Oak Vista. Dill hosts swallowtail caterpillars; tall
     * fescue hosts skipper larvae; wild violet hosts fritillary larvae.
     */
    INSECT_LARVAL_HOST,

    /**
     * Provides low-growing structural habitat at the soil surface — refugia for
     * ground beetles, spiders, and overwintering insect pupae. Distinct from
     * cover crop in that the habitat function rather than the soil-building function
     * is primary.
     */
    GROUND_COVER,

    /**
     * Planted primarily for aesthetic contribution to the garden. Many ornamentals
     * at Oak Vista also provide ecological services (Dianthus feeds pollinators)
     * and will carry additional roles alongside ORNAMENTAL.
     */
    ORNAMENTAL
}
