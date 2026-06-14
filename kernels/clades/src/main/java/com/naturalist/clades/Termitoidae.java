package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Termitoidae() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Termites are tiny pale insects that live together in huge \
            families, sometimes millions in one nest. They eat wood from \
            the inside, and some build enormous mud towers taller than a \
            grown-up. Even though people sometimes call them "white ants," \
            they are actually related to cockroaches, not ants.""",
            """
            Termitoidae is the clade containing all termites — about 3,000 \
            species of eusocial insects that evolved from wood-eating \
            cockroaches. They live in colonies with a queen, king, workers, \
            and soldiers, and they digest wood with the help of tiny \
            microbes in their guts. Termites were once given their own \
            order (Isoptera), but DNA evidence showed they are actually \
            nested inside the cockroach order Blattodea — making them, in \
            effect, social cockroaches.""",
            """
            Epifamily Termitoidae (Blattodea) — approximately 3,000 \
            described species of eusocial cockroaches, formerly classified \
            as order Isoptera. Eusociality in Termitoidae is independent \
            of Hymenoptera: termites are diploid, and caste determination \
            is primarily environmental rather than haplodiploidy-based. \
            Diagnostic features include worker and soldier castes, \
            lignocellulose digestion via hindgut flagellate or fungal \
            symbionts (Termitomyces in Macrotermitinae), and elaborate \
            nest architecture. Sister to Cryptocercus within Blattodea.""",
            """
            Epifamily Termitoidae — former order Isoptera, now an \
            infraordinal clade within Blattodea. Monophyly strongly \
            supported; sister to Cryptocercus (Lo et al. 2000; Inward et \
            al. 2007), with which it shares cellulolytic hindgut \
            flagellates (Hypermastigida, Oxymonadida). Crown-group \
            divergence in the Early Cretaceous (~130 Ma). This clade is a \
            sub-ordinal trait example: eusociality and lignocellulose \
            mutualism originated here, not at the Blattodea level. It \
            demonstrates that trait-bearing nodes need not correspond to \
            Linnaean ranks — Termitoidae has no formal rank in ICZN, yet \
            it is the correct attachment point for the eusociality trait \
            in this lineage.""");

    @Override
    public String slug() {
        return "termitoidae";
    }

    @Override
    public String displayName() {
        return "Termitoidae";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Blattodea());
    }
}
