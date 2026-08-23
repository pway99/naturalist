package com.naturalist.insects.render;

import java.util.List;

/**
 * Insects-domain paragraph cues fed to {@link com.naturalist.fieldnotes.render.DescriptionRenderer}.
 * Phrases that mark the start of a new structural paragraph in
 * entomological prose at Oak Vista &mdash; life-stage sections, host /
 * floral-access notes, management constraints, overwintering and
 * site-specific observations.
 *
 * <p>The list is intentionally starter-grade and should be extended as the
 * insect catalog grows. Add a phrase here whenever a recurrent prose
 * landmark appears in the descriptions and the renderer fails to break it
 * into its own paragraph.
 */
public final class InsectsParagraphCues {

    public static final List<String> CUES = List.of(
            "At Oak Vista",
            "Primary host guilds",
            "Adults are ",
            "Adult nectar-feeding",
            "Adult feeding biology",
            "Larval ecology",
            "Larvae are ",
            "Larval-stage",
            "Larval development",
            "Eggs hatch",
            "Egg development",
            "Pupal stage",
            "Pupal duration",
            "Pupal exposure",
            "Pupal vulnerability",
            "Puparium formation",
            "Overwintering",
            "Overwintered ",
            "Unlike ",
            "The species is ",
            "Cryptic coloration",
            "Soil microclimate",
            "Retention strategy",
            "Management:",
            "No pesticide treatment");

    private InsectsParagraphCues() {
    }
}
