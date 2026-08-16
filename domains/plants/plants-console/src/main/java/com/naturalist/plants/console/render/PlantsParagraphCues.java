package com.naturalist.plants.console.render;

import java.util.List;

/**
 * PlantSpecies-domain paragraph cues fed to {@link com.naturalist.fieldnotes.render.DescriptionRenderer}.
 * Phrases that mark the start of a new structural paragraph in botanical
 * prose at Oak Vista &mdash; bloom-period notes, management constraints,
 * symbiosis observations.
 *
 * <p>The kernel renderer is vocabulary-agnostic; this list is the plants
 * domain's contribution. Insects, chemistry, and other domains supply
 * their own cue lists from their own console modules.
 */
public final class PlantsParagraphCues {

    public static final List<String> CUES = List.of(
            "At Oak Vista",
            "CRITICAL ",
            "Management constraint",
            "Practical significance",
            "Critical timing",
            "Bloom period at",
            "Bloom time at",
            "Florets are",
            "Annual;",
            "Endophyte associations",
            "Nitrogen fixation",
            "Root nodule symbiont",
            "The low growth form",
            "Management at non-standard");

    private PlantsParagraphCues() {
    }
}
