package com.naturalist.soil.observation;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * How a soil sample was physically taken — the field procedure behind a {@link LabAnalysisInfo}.
 * Two analyses of the same bed are only comparable if they were sampled the same way, so the
 * procedure is a fact about the analysis, not a note about the day.
 * <p>
 * <b>Depth is not here.</b> It lives on {@link LabAnalysisInfo#sampleDepth()}, because depth is
 * what the lab prints on the report ("Depth: N/A" on both March 2026 reports). Carrying it in two
 * places would let the printed depth and the intended depth disagree with no way to tell which is
 * true.
 * <p>
 * Nullable on the analysis. The four March 2026 analyses have no recorded protocol, and that
 * absence is the historical truth — it must not be back-filled with a plausible-looking guess,
 * because a guessed protocol is indistinguishable from a recorded one once written down.
 */
public record SamplingProtocol(
        int subsampleCount,
        SamplingTool tool,
        CompositingMethod compositingMethod
) implements ValueObject {

    /** The implement used to draw each subsample. Extend as Oak Vista's kit changes. */
    public enum SamplingTool {
        SOIL_PROBE,
        AUGER,
        SHOVEL,
        TROWEL
    }

    /** How the subsamples were combined into the material the lab received. */
    public enum CompositingMethod {
        /** A single core, not composited. {@code subsampleCount} is 1. */
        SINGLE_CORE,
        /** Subsamples mixed in equal parts — the representative-sample default. */
        EVEN_COMPOSITE,
        /**
         * Subsamples deliberately drawn from a specific condition (a problem patch, one crop row).
         * Representative of that condition, not of the profile as a whole.
         */
        TARGETED
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .atLeast(subsampleCount, 1, "subsampleCount")
                .notNull(tool, "tool")
                .notNull(compositingMethod, "compositingMethod")
                .isTrue(compositingMethod != CompositingMethod.SINGLE_CORE || subsampleCount == 1,
                        "singleCoreImpliesOneSubsample");
    }
}
