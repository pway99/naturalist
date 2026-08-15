package com.naturalist.soil.observation;

/**
 * How a lab prints a nutrient's value relative to the substance it measures.
 * <p>
 * The distinction is not cosmetic: an oxide-equivalent figure is not the mass of the
 * element, and a reader following a nutrient row to the element it references is owed
 * that caveat. The label is what the console shows.
 */
public enum ReportedForm {

    /** The value is the element itself (calcium, zinc). */
    ELEMENTAL("reported as the element"),

    /** The value is an oxide equivalent of the element (P₂O₅ for phosphorus, K₂O for potassium). */
    OXIDE_EQUIVALENT("reported as an oxide equivalent"),

    /** The value is an ion the element occurs in (nitrate, sulfate, chloride). */
    ION("reported as an ion");

    private final String label;

    ReportedForm(String label) {
        this.label = label;
    }

    /** Reader-facing phrase for this form, e.g. for a link's title attribute. */
    public String label() {
        return label;
    }
}
