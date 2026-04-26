package com.naturalist.chemistry.compound;

/**
 * Physical-form axis — orthogonal to {@link ChemicalNature} and to functional role.
 * <p>
 * {@link #SALT} and {@link ChemicalNature#INORGANIC} are independent: NaCl is both,
 * and that is the point. The two axes do not collapse into one another.
 */
public enum PhysicalForm {
    ELEMENT,
    MINERAL,
    SALT,
    ACID,
    BASE,
    COMPLEX
}
