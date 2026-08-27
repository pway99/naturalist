package com.naturalist.usage;

/**
 * The population a {@link UsageCounter} rule is metered against: every
 * naturalist combined ({@code GLOBAL}) or one naturalist individually
 * ({@code PER_USER}).
 */
public enum UsageScope {
    GLOBAL,
    PER_USER
}
