package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Monocots() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Grasses, lilies, and onions are plants whose baby leaf comes up as \
            a single blade. These are called monocots.""",
            """
            Monocots are flowering plants that sprout with just one seed-leaf. \
            You can often spot them by their long, narrow leaves with parallel \
            lines running down them, and by flower parts that come in threes. \
            Grasses, corn, lilies, palms, and onions are all monocots.""",
            """
            Monocots (Monocotyledons) are flowering plants defined by a single \
            embryonic seed-leaf. They usually have parallel-veined leaves, \
            floral parts in multiples of three, vascular bundles scattered \
            through the stem, and fibrous roots. Grasses, sedges, orchids, \
            palms, and lilies make up this clade of roughly 70,000 species.""",
            """
            Monocotyledoneae — a strongly supported clade nested within \
            angiosperms, sister to the eudicot lineage in most topologies. \
            Synapomorphies include a single cotyledon, retained monosulcate \
            pollen, sympodial growth, an atactostele, and loss of the bifacial \
            vascular cambium (hence no conventional secondary woody growth). \
            The Oak Vista catalog reaches this lineage through its subclade \
            commelinids; monocots is retained as the parent node on that walk. \
            Its parent is Angiosperms.""");

    @Override
    public String slug() {
        return "monocots";
    }

    @Override
    public String displayName() {
        return "Monocots";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Angiosperms());
    }
}
