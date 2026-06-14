package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Sophophora() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            There is one very special tiny fruit fly that scientists have \
            studied more than almost any other animal. It has helped us \
            learn how our eyes, hearts, and brains are built. It belongs \
            to a small group of related flies called Sophophora.""",
            """
            Sophophora is a subgenus — a smaller group within the pomace \
            flies — that contains Drosophila melanogaster, the world's most \
            studied insect. Scientists call it the "lab fruit fly." By \
            studying its genetics for over a century, we've learned how \
            genes control eye colour, body plans, and even behaviour. \
            Sophophora flies are found worldwide and are closely related \
            to, but distinct from, the subgroup that contains the "type" \
            fruit fly, D. funebris.""",
            """
            Subgenus Sophophora (Drosophilinae) — contains D. melanogaster \
            and approximately 330 described species. Melanogaster is the \
            preeminent genetic model organism: the first animal to have a \
            gene mapped to a chromosome (Morgan 1910), and the basis for \
            foundational work in developmental genetics, neurobiology, and \
            ageing. Under current ICZN rules (Case 3407 context), \
            melanogaster does not reside in Drosophila sensu stricto \
            because the type species D. funebris anchors the name to a \
            different clade. Sophophora is therefore far from the nominal \
            subgenus despite its overwhelming prominence in the \
            literature.""",
            """
            Subgenus Sophophora Sturtevant 1939 — the melanogaster species \
            group and allies. Crown-group divergence in the Palaeogene; \
            the melanogaster subgroup radiated in sub-Saharan Africa in the \
            late Miocene. ICZN Case 3407 (Drosophila vs. Sophophora) \
            proposed conserving the name Drosophila for the melanogaster \
            clade by suppressing the type fixation of D. funebris; the \
            Commission declined (Opinion 2245, 2010 — though deliberations \
            remain contested). This clade node exists to anchor the \
            model-organism lineage independently of the unresolved \
            nomenclatural status, allowing downstream trait declarations \
            (e.g. cosmopolitan synanthropy, vinegar-substrate ecology) to \
            attach here without implying genus-rank identity.""");

    @Override
    public String slug() {
        return "sophophora";
    }

    @Override
    public String displayName() {
        return "Sophophora";
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
