package com.naturalist.usage;

import com.naturalist.naturalist.NaturalistName;

/**
 * Seam for the future paid/credit bucket: whether a naturalist is entitled to
 * bypass the public usage rules {@link UsageCommand#reserve} enforces. Today
 * always false (every naturalist is in the public bucket) — see {@link #none()}.
 */
public interface EntitlementLookup {

    boolean isEntitled(NaturalistName naturalist);

    /** No naturalist is entitled. The production wiring today. */
    static EntitlementLookup none() {
        return naturalist -> false;
    }
}
