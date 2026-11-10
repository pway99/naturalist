package com.naturalist.insects;

import com.naturalist.library.CladeStep;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * The phylogenetic breadcrumb row for one insect page: the real clade nodes
 * (root → subject) plus an optional gap label naming the current catalog entity
 * when it has no clade of its own.
 */
public record CladeTrail(List<CladeStep> steps, String gapLabel) {

    public boolean hasGap() {
        return StringUtils.isNotBlank(gapLabel);
    }
}
