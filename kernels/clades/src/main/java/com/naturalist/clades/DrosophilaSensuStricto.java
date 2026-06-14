package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record DrosophilaSensuStricto() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            This is the group of tiny fruit flies that keeps the official \
            name "Drosophila" — the real, original Drosophila. It includes \
            the very first fly scientists named Drosophila, called D. \
            funebris, which you might find near compost bins.""",
            """
            Drosophila sensu stricto means "Drosophila in the strict sense" \
            — it is the subgenus that contains the type species D. funebris, \
            the fly that was originally given the name Drosophila back in \
            1787. Under naming rules, whichever group contains the original \
            type species gets to keep the name. So even though the famous \
            lab fruit fly (D. melanogaster) is more well-known, it actually \
            belongs to a different subgroup called Sophophora, not to the \
            "true" Drosophila.""",
            """
            Drosophila sensu stricto — the subgenus containing the type \
            species D. funebris Fabricius 1787, which fixes the name \
            Drosophila to this clade under ICZN nomenclatural rules. The \
            subgenus is much less studied than the melanogaster group \
            (Sophophora), but it is the nomenclatural anchor: D. funebris \
            was the species on which Fallén (1823) based the genus, and \
            principle of typification means the genus name follows the type. \
            ICZN Case 3407 sought to overturn this by conserving \
            melanogaster as the type; the proposal was not accepted.""",
            """
            Drosophila sensu stricto — nominal subgenus containing D. \
            funebris (type species of Drosophila Fallén 1823). Under ICZN \
            Art. 61.2, the genus-group name is permanently attached to its \
            type species; the massive paraphyly of Drosophila sensu lato \
            means that if the genus is split, only the funebris clade \
            retains the name Drosophila. O'Grady & DeSalle (2018) proposed \
            a comprehensive reclassification elevating most subgenera to \
            genus rank; adoption remains partial across the community. This \
            clade node records the ICZN-correct interpretation — \
            Drosophila = funebris group — independently of whether the \
            community adopts the split. It is the strongest case in this \
            kernel of clade ≠ colloquial usage.""");

    @Override
    public String slug() {
        return "drosophila-sensu-stricto";
    }

    @Override
    public String displayName() {
        return "Drosophila sensu stricto";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Drosophilinae());
    }
}
