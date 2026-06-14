package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Drosophilinae() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Fruit flies are tiny flies you sometimes see buzzing around \
            ripe bananas and other fruit on the kitchen counter. Scientists \
            study them because they grow up very fast and teach us how \
            living things work.""",
            """
            Drosophilinae is a subfamily of small flies called pomace flies \
            or vinegar flies — the ones you see hovering around overripe \
            fruit. The most famous member is Drosophila melanogaster, the \
            tiny fly scientists have studied for over a hundred years to \
            understand genetics and how bodies develop. There are about \
            4,000 species of drosophilines, living on every continent \
            except Antarctica, and they feed on yeasts growing on decaying \
            fruit, sap, and fungi.""",
            """
            Subfamily Drosophilinae (Diptera: Drosophilidae) — the largest \
            subfamily of pomace flies, containing roughly 4,000 species \
            including the paraphyletic genus Drosophila. Members breed on \
            fermenting substrates — decaying fruit, sap fluxes, fungi — \
            and feed primarily on associated yeasts. The traditional genus \
            Drosophila is paraphyletic: molecular phylogenetics reveals \
            that multiple distinct lineages (e.g. the Hawaiian Idiomyia \
            radiation, Scaptomyza, Zaprionus) are embedded within it. \
            This subfamily node represents the containing crown clade for \
            that paraphyletic assemblage.""",
            """
            Subfamily Drosophilinae — crown clade for the paraphyletic \
            genus Drosophila and its embedded lineages. van der Linde et al. \
            (2010) and subsequent genomic analyses confirm massive \
            paraphyly: Scaptomyza (leaf-mining flies), the Hawaiian \
            Drosophila + Idiomyia radiation (~1,000 spp.), and Zaprionus \
            all nest within Drosophila sensu lato. The ICZN Case 3407 \
            petition to conserve melanogaster in Drosophila (by redefining \
            the type species) was not approved, leaving D. funebris as \
            the type; the subgenus Sophophora (containing melanogaster) is \
            therefore nomenclaturally distinct from Drosophila sensu \
            stricto. This node is the trait-bearing ancestor for pomace-fly \
            ecology without committing to a particular genus-level \
            resolution of the paraphyly.""");

    @Override
    public String slug() {
        return "drosophilinae";
    }

    @Override
    public String displayName() {
        return "Drosophilinae";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Holometabola());
    }
}
