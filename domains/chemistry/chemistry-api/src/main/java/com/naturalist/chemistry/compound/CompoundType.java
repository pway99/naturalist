package com.naturalist.chemistry.compound;

public enum CompoundType {
    INORGANIC_SALT,      // CaSO4, KCl, MgSO4
    ORGANIC_ACID,        // HCOOH, H2C2O4, organic acid chelates
    MINERAL,             // CaCO3, elemental S
    ELEMENT,             // pure elemental (S, Cu)
    CHELATE,             // organic acid chelated metal complex
    BIOLOGICAL_COMPOUND, // thymol, azadirachtin, fatty acids
    VOLATILE_ORGANIC     // fumigants — formic acid, thymol
}
