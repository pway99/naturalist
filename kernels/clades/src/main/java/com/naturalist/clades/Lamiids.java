package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Lamiids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Tomatoes, mint, sage, and borage are lamiids.""",
            """
            Lamiids are one half of the asterids. They include the mint and \
            sage family, tomatoes and nightshades, borage, and coffee. Many \
            lamiids have square stems or strongly scented leaves.""",
            """
            Lamiids (euasterids I) are an asterid subclade containing Lamiales \
            (mints, sages), Solanales (tomatoes, nightshades), Boraginales \
            (borage), Gentianales (coffee), and Garryales. They often bear \
            opposite leaves and flowers with fused, two-lipped corollas, and \
            are rich in alkaloids and aromatic oils.""",
            """
            Lamiidae (lamiids / euasterids I) — core-asterid clade comprising \
            Lamiales, Solanales, Boraginales, Gentianales, Garryales, \
            Vahliales, and Icacinales, broadly marked by opposite leaves, \
            decussate phyllotaxy, and frequent zygomorphy. In the Oak Vista \
            catalog lamiids places three orders: Solanales (Solanum \
            lycopersicum), Lamiales (Thymus, Salvia), and Boraginales \
            (Borago). Its parent is Asterids.""");

    @Override
    public String slug() {
        return "lamiids";
    }

    @Override
    public String displayName() {
        return "Lamiids";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Asterids());
    }
}
