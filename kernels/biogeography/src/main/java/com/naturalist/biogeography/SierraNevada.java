package com.naturalist.biogeography;

import com.naturalist.fieldnotes.Description;

public record SierraNevada() implements Bioregion {

    private static final Description DESCRIPTION = new Description(
            "The Sierra Nevada are giant snowy mountains with the biggest trees in the world — " +
                    "the giant sequoias. Bears wander the forests, and tiny pikas live in the " +
                    "rocks high up where the snow stays late into summer.",
            "The Sierra Nevada is a long high mountain range running down the eastern side of " +
                    "California. The granite peaks rise more than 14,000 feet, with deep U-shaped " +
                    "valleys carved by glaciers. Forests of pine, fir, and the giant sequoia trees " +
                    "cover the slopes; alpine meadows and stark granite peaks are above. Snowpack " +
                    "in winter feeds the Sacramento Valley and most of California's agriculture.",
            "The Sierra Nevada is a 640 km north–south granitic batholith forming California's " +
                    "principal eastern mountain range, with peaks exceeding 4,200 m at Mount " +
                    "Whitney and Mount Williamson. The range is asymmetric: a long gradual " +
                    "western slope draining to the Central Valley and a steep eastern escarpment " +
                    "dropping to the Owens Valley along the Sierra Nevada Frontal Fault. Forest " +
                    "communities follow elevational zonation — foothill oak woodland, ponderosa " +
                    "pine and mixed-conifer forest, red fir, subalpine forest, and treeless " +
                    "alpine zone above ~3,300 m. Sequoiadendron giganteum (giant sequoia) groves " +
                    "occur in a narrow band on the western slope at 1,400–2,150 m. Winter " +
                    "snowpack accumulates 150–250% of liquid-equivalent annual precipitation in " +
                    "the headwaters and supplies most of California's freshwater.",
            "The Sierra Nevada corresponds to EPA Level III Ecoregion 5. The range is the largest " +
                    "single block of exposed granitic plutonic rock in North America, formed by " +
                    "Cretaceous arc magmatism above the subducting Farallon plate (~115–80 Ma) " +
                    "and uplifted as a tilted fault block beginning in the late Cenozoic " +
                    "(~5 Ma). The exposed Sierra Nevada batholith comprises hundreds of " +
                    "individual plutons of granodiorite, tonalite, and granite. Glacial " +
                    "modification during Pleistocene maxima carved characteristic U-shaped " +
                    "valleys (Yosemite, Hetch Hetchy, Tehipite), cirques, arêtes, and tarns. " +
                    "Sequoiadendron giganteum is monotypic in genus and restricted to ~75 groves " +
                    "on the western slope; reproduction requires fire to release seeds from " +
                    "serotinous cones. Vertebrate fauna includes Ursus americanus, Vulpes vulpes " +
                    "necator (Sierra Nevada red fox, critically endangered), Ochotona princeps " +
                    "(American pika, alpine talus specialist), Picoides arcticus (black-backed " +
                    "woodpecker, post-fire specialist), and the endemic Mountain yellow-legged " +
                    "frog (Rana sierrae). Snowpack hydrology is the principal water supply for " +
                    "the Central Valley and southern California; loss of snowpack to a rain-" +
                    "dominated precipitation regime is the dominant climate-change concern for " +
                    "California water systems."
    );

    @Override
    public String slug() {
        return "sierra-nevada";
    }

    @Override
    public String displayName() {
        return "Sierra Nevada";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }
}
