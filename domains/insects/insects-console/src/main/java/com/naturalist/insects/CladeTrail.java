package com.naturalist.insects;

import com.naturalist.library.CladeStep;

import java.util.List;

/**
 * The phylogenetic breadcrumb row for one insect page: the real clade nodes
 * (root → subject) plus an optional gap label naming the current catalog entity
 * when it has no clade of its own.
 */
public record CladeTrail(List<CladeStep> steps, String gapLabel) {

    public boolean hasGap() {
        return gapLabel != null && !gapLabel.isBlank();
    }
}
