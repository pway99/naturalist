package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Hemiptera() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Some bugs drink the juice from plants or other animals through \
            a tiny straw on their face. Leafhoppers, aphids, and stink bugs \
            are all true bugs. Their babies look like little wingless copies \
            of their parents.""",
            """
            True bugs all share one tool: a piercing-sucking mouth that \
            works like a hypodermic needle, used to drink fluids — plant \
            sap, animal blood, or sometimes another bug. Leafhoppers, \
            cicadas, aphids, stink bugs, water striders, and bed bugs are \
            all hemipterans. Unlike butterflies or beetles, they do not go \
            through a caterpillar or grub stage; a newly hatched nymph \
            already looks like a small wingless adult and gains wings only \
            at the final moult.""",
            """
            Hemiptera is the largest hemimetabolous order — over 80,000 \
            described species. Diagnostic feature: a piercing-sucking \
            rostrum (stylet bundle housed in a labial sheath) used to feed \
            on phloem, xylem, plant tissue, or animal hemolymph. The order \
            subdivides into Heteroptera (true bugs sensu stricto — stink \
            bugs, assassin bugs, water striders), Auchenorrhyncha (cicadas, \
            leafhoppers, planthoppers — phloem and xylem feeders), and \
            Sternorrhyncha (aphids, whiteflies, scale insects, psyllids — \
            soft-bodied phloem feeders). All hemipterans share \
            hemimetabolous development; nymphs and adults occupy the same \
            trophic niche, distinguishing them from holometabolan groups \
            whose larval and adult roles diverge sharply.""",
            """
            Order Hemiptera within Paraneoptera — sister to Thysanoptera \
            (thrips) and the psocodean ancestors of lice. Crown group from \
            the Early Permian; the three suborders (Heteroptera, \
            Auchenorrhyncha, Sternorrhyncha) are well-supported as \
            monophyletic, with traditional 'Homoptera' now recognised as \
            paraphyletic. Mouthpart apomorphy: mandibles and maxillae \
            modified into stylets that interlock to form a food canal \
            (drawing fluid up) and a salivary canal (delivering enzymes \
            downward); the labium forms a non-piercing sheath. Salivary \
            chemistry varies by feeding guild — phytophagous hemipterans \
            inject pectinases, cellulases, and proteinases that degrade \
            plant cell walls and trigger systemic responses (hopperburn, \
            gall formation, plant-virus transmission); haematophagous taxa \
            inject anticoagulants. Lifecycle is uniformly hemimetabolous \
            across the order — egg, ~5 nymphal instars, adult — which is \
            what justifies declaring MetabolyTrait(Hemimetabolous) at the \
            order node rather than at every constituent family.""");

    @Override
    public String slug() {
        return "hemiptera";
    }

    @Override
    public String displayName() {
        return "Hemiptera";
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
