package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Lepidoptera() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Butterflies and moths are insects with big wings covered in \
            tiny coloured scales. The scales rub off like dust if you \
            touch them. Caterpillars grow up to become butterflies and \
            moths.""",
            """
            Lepidoptera is the order of insects we call butterflies and \
            moths. Their wings are covered with millions of overlapping \
            flat scales, which give them their colours and patterns. \
            Caterpillars (the larvae) eat leaves, while most adults sip \
            nectar from flowers using a long coiled mouthpart called a \
            proboscis. Butterflies are usually active during the day and \
            have club-shaped antennae; most moths are active at night and \
            have feathery or thread-like antennae.""",
            """
            Order Lepidoptera contains approximately 180,000 described \
            species across about 130 families, second only to Coleoptera \
            in described insect diversity. Diagnostic features include \
            wings covered in flat overlapping scales (modified setae), a \
            coiled proboscis (haustellum) in most adults, and chewing \
            mandibulate larvae. Larvae feed almost exclusively on plant \
            tissues — Lepidoptera is the dominant herbivorous insect order \
            on land and a primary driver of plant chemical defence \
            evolution. Adults are nectar feeders or, in some lineages, \
            non-feeding.""",
            """
            Order Lepidoptera (Holometabola: Amphiesmenoptera, sister to \
            Trichoptera) — earliest fossils from the Late Triassic \
            (~200 Ma); major radiation tracks the rise of angiosperms in \
            the Cretaceous. Major superfamilies relevant to Oak Vista \
            include Papilionoidea (butterflies and skippers — Papilionidae, \
            Nymphalidae, Pieridae, Lycaenidae, Hesperiidae), Noctuoidea \
            (owlet moths), Geometroidea (geometer moths), and Bombycoidea \
            (silk moths, hawkmoths). Larval-host-plant specificity ranges \
            from extreme monophagy (single host genus) to broad polyphagy; \
            the host range distribution shapes most agricultural and \
            conservation considerations involving the order.""");

    @Override
    public String slug() {
        return "lepidoptera";
    }

    @Override
    public String displayName() {
        return "Lepidoptera";
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
