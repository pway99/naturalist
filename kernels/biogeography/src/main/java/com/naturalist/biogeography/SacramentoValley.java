package com.naturalist.biogeography;

import com.naturalist.fieldnotes.Description;

public record SacramentoValley() implements Bioregion {

    private static final Description DESCRIPTION = new Description(
            "The Sacramento Valley is a big flat sunny place where lots of food grows. " +
                    "The Sacramento River runs right down the middle, and tall oak trees give shade. " +
                    "Summer is very hot and winter brings the rains.",
            "The Sacramento Valley is the northern half of California's Central Valley — a wide, " +
                    "flat floor between the Coast Ranges and the Sierra Nevada, drained by the " +
                    "Sacramento River. Hot dry summers and cool wet winters define a Mediterranean " +
                    "climate. Native vegetation is valley oak savanna, riparian forest along the " +
                    "rivers, and seasonal vernal pool grasslands; the ecology has been heavily " +
                    "transformed by agriculture.",
            "The Sacramento Valley is a structural sediment basin between the Coast Ranges and " +
                    "the Sierra Nevada, containing the lower Sacramento River system. Mediterranean " +
                    "climate (Csa): long hot dry summers (peak temperatures regularly above 38 °C) " +
                    "and cool wet winters (mean January lows near 3 °C). Pre-agricultural plant " +
                    "communities were dominated by Quercus lobata savanna, Populus fremontii / " +
                    "Salix gooddingii riparian gallery forest, and seasonal vernal pool / annual " +
                    "grassland mosaics on heavy clay soils. The valley is now California's most " +
                    "intensively cultivated landscape, but remnant native communities persist along " +
                    "watercourses and on conservation lands.",
            "The Sacramento Valley corresponds approximately to EPA Level III Ecoregion 7 " +
                    "(Central California Valley, northern subregion). Quaternary alluvium overlies " +
                    "Tertiary marine and continental sediments in a structural foredeep between " +
                    "the Franciscan Complex of the Coast Ranges and the Sierran granitic batholith. " +
                    "Pre-Columbian fire regimes (1–10 year return intervals, indigenous burning) " +
                    "maintained Quercus lobata–Quercus douglasii savanna and tarweed-dominated " +
                    "annual grasslands; post-1850 conversion to irrigated agriculture has reduced " +
                    "native habitat to <5% of historical extent. Endemic and characteristic taxa " +
                    "include Arctostaphylos manzanita, Aristolochia californica (host of Battus " +
                    "philenor hirsuta), and the Sacramento perch (Archoplites interruptus). The " +
                    "Sacramento River system supports anadromous Oncorhynchus tshawytscha runs " +
                    "(Chinook salmon) on a depleted but extant basis."
    );

    @Override
    public String slug() {
        return "sacramento-valley";
    }

    @Override
    public String displayName() {
        return "Sacramento Valley";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }
}
