package com.naturalist.biogeography;

import com.naturalist.fieldnotes.Description;

public record CoastRanges() implements Bioregion {

    private static final Description DESCRIPTION = new Description(
            "The Coast Ranges are the green hills and mountains right next to the ocean. " +
                    "Fog drifts in every morning and waters the giant redwood trees. Otters " +
                    "swim in the kelp forests just offshore.",
            "The Coast Ranges run along California's coastline north of San Francisco Bay. " +
                    "Cool ocean fog rolls in almost every summer morning and is what allows the " +
                    "world's tallest trees, the coast redwoods, to grow here. The hills are " +
                    "covered with mixed evergreen forest, grasslands, and oak woodlands. The " +
                    "rocky coast and offshore kelp beds support sea otters, harbour seals, and " +
                    "abundant seabirds.",
            "The Northern California Coast Ranges are a system of coast-parallel mountain " +
                    "ridges and valleys formed by Cenozoic compression along the San Andreas " +
                    "transform plate boundary. Summer marine fog (advection fog driven by the " +
                    "California Current upwelling system) supplies a substantial fraction of " +
                    "annual moisture to coastal vegetation, particularly the Sequoia sempervirens " +
                    "(coast redwood) belt. Inland communities transition through Douglas-fir / " +
                    "tanoak forest, mixed evergreen forest, oak woodland (Quercus agrifolia, " +
                    "Q. kelloggii), and California coastal prairie grasslands. Marine ecosystems " +
                    "are dominated by Macrocystis pyrifera (giant kelp) forests supporting " +
                    "Enhydra lutris (sea otter), Phoca vitulina (harbour seal), and a high-" +
                    "diversity intertidal invertebrate fauna.",
            "The Coast Ranges of northern and central California span EPA Level III Ecoregions " +
                    "1 (Coast Range) and 6 (Central California Foothills and Coastal Mountains). " +
                    "Bedrock is dominated by the Franciscan Complex — a Mesozoic accretionary " +
                    "wedge of greywacke, chert, basalt, and serpentinite formed at the Farallon " +
                    "subduction margin — overlain by Cenozoic Coast Range sediments and active " +
                    "Quaternary deformation along the San Andreas, Hayward, and Maacama faults. " +
                    "The Sequoia sempervirens fog belt extends from southern Oregon to Big Sur, " +
                    "constrained climatically by the summer marine layer and topographically by " +
                    "elevations below ~750 m. Notable endemic and characteristic taxa include " +
                    "Aristolochia californica (host of Battus philenor hirsuta — the same vine " +
                    "established at Oak Vista), Sequoia sempervirens, Sequoiadendron-relict " +
                    "associated mycorrhizal communities, and the California red-legged frog " +
                    "(Rana draytonii). The fog-driven hydrology is increasingly under threat " +
                    "from changes in the strength and frequency of coastal upwelling under " +
                    "anthropogenic climate forcing."
    );

    @Override
    public String slug() {
        return "coast-ranges";
    }

    @Override
    public String displayName() {
        return "Coast Ranges";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }
}
