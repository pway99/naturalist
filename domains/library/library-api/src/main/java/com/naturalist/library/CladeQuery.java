package com.naturalist.library;

import com.naturalist.taxonomy.LinealRank;

import java.util.List;
import java.util.Optional;

public interface CladeQuery {

    Optional<CladeView> getBySlug(String slug);

    CladeTreeNode tree();

    record CladeTreeNode(
            String slug,
            String displayName,
            Optional<LinealRank> rank,
            List<CladeTreeNode> children
    ) {}
}
