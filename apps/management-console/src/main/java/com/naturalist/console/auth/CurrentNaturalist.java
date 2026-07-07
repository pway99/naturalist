package com.naturalist.console.auth;

import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistName;

import java.util.Optional;

/**
 * Session-identity seam. The only Java-facing way to learn who is acting.
 * Consumers (e.g. the future collection feature) depend on this, never on
 * {@code SecurityContextHolder} directly (design: seam discipline).
 */
public interface CurrentNaturalist {

    Optional<NaturalistName> name();

    Optional<Naturalist> naturalist();
}
