package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Magnoliids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Some flowering plants are a very old kind, like the pipevine that \
            climbs in shady spots. These old-fashioned flowering plants are \
            called magnoliids.""",
            """
            Magnoliids are one of the oldest branches of the flowering-plant \
            family. They include magnolias, bay laurel, black pepper, and the \
            pipevine. Their flowers often have parts in threes and follow an \
            older plan than the flowers of daisies or roses.""",
            """
            Magnoliids are a lineage of about 10,000 flowering-plant species \
            that branched off before the split between monocots and eudicots. \
            Members — magnolias, laurels, the peppers and pipevines of \
            Piperales — typically have floral parts in multiples of three and \
            pollen with a single furrow, features they share with monocots but \
            not with eudicots.""",
            """
            Magnoliidae (magnoliids sensu APG IV) — a clade of four orders \
            (Canellales, Piperales, Laurales, Magnoliales) recovered outside \
            the monocot + eudicot node. Plesiomorphically trimerous, often \
            spirally arranged floral organs and monosulcate pollen. In the Oak \
            Vista catalog this node places Piperales — Aristolochia californica, \
            the California pipevine and larval host of Battus philenor. The \
            placement is the correct current answer even though magnoliids and \
            angiosperms above it carry no Linnaean rank.""");

    @Override
    public String slug() {
        return "magnoliids";
    }

    @Override
    public String displayName() {
        return "Magnoliids";
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
