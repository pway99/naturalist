package com.naturalist.chemistry.reaction;

enum ReactionType {
    DISSOLUTION,       // solid dissolving in water — gypsum dissolution
    ACID_BASE,         // neutralization — H2SO4 + CaCO3
    OXIDATION,         // sulfur oxidation by Thiobacillus
    CATION_EXCHANGE,   // Ca2+ displacing K+ from exchange sites
    CHELATION,         // metal ion binding to organic acid
    VOLATILIZATION,    // liquid/solid to vapor phase
    BIOLOGICAL,        // enzyme or organism catalyzed
    PRECIPITATION      // ions forming insoluble solid
}
