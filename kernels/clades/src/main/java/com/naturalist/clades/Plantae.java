package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Plantae() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Grass, flowers, trees, and moss are all plants. They stay in one \
            place and make their own food out of sunlight. Every plant belongs \
            to one big group called Plantae.""",
            """
            Plantae is the plant kingdom. Plants are living things that stay \
            rooted in one spot, have green parts that catch sunlight, and use \
            that light to build their own sugary food — a trick called \
            photosynthesis. Mosses, ferns, pine trees, and flowering plants \
            are all part of Plantae.""",
            """
            Kingdom Plantae contains the land plants and their closest \
            green-algal relatives: multicellular photosynthetic eukaryotes \
            whose chloroplasts descend from an ancient captured cyanobacterium. \
            Plants build their bodies from cellulose cell walls and, in most \
            lineages, move water through specialised vascular tissue. The \
            kingdom spans mosses, ferns, gymnosperms such as conifers, and the \
            flowering plants.""",
            """
            Kingdom Plantae — the embryophyte clade together with the \
            charophyte green algae, united by primary plastids from a single \
            cyanobacterial endosymbiosis and by chlorophyll a + b. This node \
            is the plant-side entry point that rejoins the animal lineage at \
            Eukaryota; the intermediate grades (Viridiplantae, Streptophyta, \
            Embryophyta, Tracheophyta, Spermatophyta) are collapsed here and \
            the kernel adds them only when a catalogued taxon demands the finer \
            resolution. Photosynthesis is the trait a plants consumer would \
            declare at or below this node; the kernel itself holds none.""");

    @Override
    public String slug() {
        return "plantae";
    }

    @Override
    public String displayName() {
        return "Plantae";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Eukaryota());
    }
}
