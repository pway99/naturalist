package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Insecta() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Insects are little animals with six legs and a body in three \
            pieces — a head, a middle, and a back end. Many of them have \
            wings, like flies and butterflies and bees.""",
            """
            Insecta is the part of the arthropod tree that has six legs \
            and a body split into three parts: head, thorax, and abdomen. \
            Most insects have a pair of antennae on their head and one or \
            two pairs of wings as adults. Bees, beetles, ants, flies, \
            dragonflies, grasshoppers, and butterflies are all insects. \
            Spiders are not insects — they have eight legs and a different \
            body plan.""",
            """
            Class Insecta is the most species-rich clade of animals, with \
            over a million described species. Diagnostic features include \
            a three-tagma body (head, thorax, abdomen), three pairs of \
            thoracic legs, one pair of antennae, and (in most orders) two \
            pairs of wings on the meso- and metathorax. Insects are the \
            only invertebrates that have evolved powered flight, and that \
            innovation is associated with the diversification of every \
            major terrestrial habitat. Sub-clades range from primitive \
            wingless silverfish (Zygentoma) and hemimetabolous dragonflies \
            (Odonata) and bugs (Hemiptera) through to the highly diverse \
            Holometabola.""",
            """
            Class Insecta within Hexapoda — sister to Entognatha \
            (springtails, proturans, diplurans). Crown-group fossils from \
            the Early Devonian Rhynie chert (Rhyniognatha hirsti, ~410 Ma); \
            winged insects (Pterygota) by the Carboniferous. Cladogram \
            order: Archaeognatha → Zygentoma → Palaeoptera (Odonata + \
            Ephemeroptera) → Neoptera, with Neoptera subdivided into \
            Polyneoptera (orthopteroids and plecopteroids), Paraneoptera \
            (hemipteroids), and Holometabola. Tracheal respiration via \
            spiracles, malpighian tubule excretion, and a hexapodous gait \
            are class-level synapomorphies; metaboly is not — that \
            originates one node further down in Holometabola, which is \
            precisely why this kernel routes life-stage modelling through \
            clade traversal rather than smearing it across Insecta.""");

    @Override
    public String slug() {
        return "insecta";
    }

    @Override
    public String displayName() {
        return "Insecta";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Arthropoda());
    }
}
