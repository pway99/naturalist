package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Rosids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Apple trees, roses, and pea plants are all rosids.""",
            """
            Rosids are a huge group of eudicots. They include roses and their \
            relatives, bean plants, oaks, maples, and most of the trees in a \
            temperate forest. Many of our orchard fruits are rosids.""",
            """
            Rosids are one of the largest clades of flowering plants, holding \
            roughly a quarter of all angiosperm species. They divide into two \
            big groups — the fabids and the malvids. Rosids include legumes, \
            roses, oaks, willows, citrus, and mallows, and many are woody or \
            form nitrogen-fixing partnerships with soil bacteria.""",
            """
            Rosidae (rosids) — a core-eudicot clade sister to Vitales within \
            superrosids, comprising the fabid (eurosids I) and malvid \
            (eurosids II) subclades. Broadly characterised by ellagic acid, by \
            mycorrhizal and rhizobial/actinorhizal nitrogen-fixing symbioses \
            concentrated in the fabid "nitrogen-fixing clade," and by a \
            frequent woody habit. The Oak Vista catalog reaches both daughter \
            clades; rosids is their shared parent. Its parent is Superrosids.""");

    @Override
    public String slug() {
        return "rosids";
    }

    @Override
    public String displayName() {
        return "Rosids";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Superrosids());
    }
}
