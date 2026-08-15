package com.naturalist.soil.observation;

/**
 * Who made a statement the console shows. Every assessment carries one, and the console prints it
 * — a reader must never have to guess whether a verdict came from the laboratory or from this
 * application.
 * <p>
 * The rule exists because the two can disagree. Box 1's zinc reads 6.35 lbs/1000 ft² against a
 * printed optimum of 0.39–4.0, so arithmetic over the lab's own two numbers says "above range" —
 * while FGL's own graphical bar for that row reads as satisfactory. The lab's five-band verdict
 * uses information it did not print as numbers, and we do not have it. Presenting our arithmetic
 * as the lab's judgment would put words in their mouth.
 */
public enum AssessmentSource {

    /** Printed on the report. A transcription, not a computation. */
    LAB_PRINTED("as printed by the lab"),

    /**
     * Computed here, by comparing a printed measurement against a printed optimum range. Uses
     * only the lab's numbers, but the conclusion is ours and can differ from the lab's own band.
     */
    DERIVED_FROM_PRINTED_RANGE("our comparison against the lab's printed range");

    private final String label;

    AssessmentSource(String label) {
        this.label = label;
    }

    /** Human-readable attribution, for display beside the assessment. */
    public String label() {
        return label;
    }
}
