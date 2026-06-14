package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Blattodea() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Cockroaches are flat brown insects that scurry away when you \
            turn on the light. They have long feelers and can run very \
            fast. Termites — the tiny insects that eat wood — are actually \
            a special kind of cockroach that lives in big groups.""",
            """
            Blattodea is the order that includes all cockroaches and \
            termites. Most cockroaches are flat-bodied, fast-running \
            scavengers with long antennae. They are hemimetabolous — their \
            young look like small adults and grow by moulting, without a \
            pupa stage. Termites were once placed in their own order \
            (Isoptera), but scientists discovered that they evolved from \
            within cockroaches, making them essentially social cockroaches \
            that eat wood. There are about 4,600 cockroach species and \
            3,000 termite species.""",
            """
            Order Blattodea — cockroaches and termites. Hemimetabolous; \
            development is direct (egg → nymph → adult) with no pupal \
            stage. The order is defined by the inclusion of Isoptera \
            (termites) as an infraorder nested within Blattodea, rendering \
            traditional "cockroaches" paraphyletic: cockroaches excluding \
            termites do not form a clade. Termites are the sister group of \
            Cryptocercus (wood-eating cockroaches), sharing hindgut \
            flagellate symbionts for cellulose digestion. Cockroaches \
            display generalist detritivory, nocturnal habit, and \
            dorsoventrally flattened body form; termites are eusocial with \
            caste differentiation.""",
            """
            Order Blattodea — hemimetabolous Polyneoptera; sister to \
            Mantodea within Dictyoptera. The nesting of Isoptera within \
            Blattodea (Inward et al. 2007; Lo et al. 2000) is the \
            textbook example of a ranked taxon (order Isoptera) being \
            subsumed when phylogenetics reveals it as an internal clade. \
            This node is the trait-bearing ancestor for hemimetabolous \
            development in the cockroach lineage — distinct from the \
            independent hemimetabolous condition in Hemiptera. Fossil \
            record extends to the Carboniferous (~320 Ma); crown-group \
            Blattodea diversified in the Mesozoic. The clade is a \
            paraphyly-fixture node: "cockroach" is paraphyletic, \
            "Blattodea" is monophyletic only because it includes \
            termites.""");

    @Override
    public String slug() {
        return "blattodea";
    }

    @Override
    public String displayName() {
        return "Blattodea";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Insecta());
    }
}
