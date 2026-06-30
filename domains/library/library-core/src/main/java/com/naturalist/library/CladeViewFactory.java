package com.naturalist.library;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeCatalog;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.List;
import java.util.Optional;

class CladeViewFactory {

    private static final Observer observer = Observer.forClass(CladeViewFactory.class);

    Optional<CladeView> buildBySlug(String slug) {
        observer.arguments("buildBySlug", i -> i.notBlank(slug, "slug")).throwWhenInvalid();
        Clade clade;
        try {
            clade = Clade.of(slug);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        List<Clade> chain = CladeTraversal.ancestry(clade); // [clade, parent, ..., root]
        List<CladeStep> ancestry = chain.reversed().stream()
                .filter(c -> !c.equals(clade))
                .map(CladeViewFactory::toStep)
                .toList();

        CladeStep subject = toStep(clade);

        List<CladeStep> children = CladeCatalog.childrenOf(clade).stream()
                .map(CladeViewFactory::toStep)
                .toList();

        CladeView view = new CladeView(subject, ancestry, children);
        observer.observable(view, "cladeView").observe(Level.WARN);
        return Optional.of(view);
    }

    CladeQuery.CladeTreeNode buildTree() {
        return buildTreeNode(new Eukaryota());
    }

    private CladeQuery.CladeTreeNode buildTreeNode(Clade clade) {
        List<CladeQuery.CladeTreeNode> children = CladeCatalog.childrenOf(clade).stream()
                .map(this::buildTreeNode)
                .toList();
        return new CladeQuery.CladeTreeNode(
                clade.slug(), clade.displayName(), CladeRanks.rankFor(clade), children);
    }

    private static CladeStep toStep(Clade clade) {
        return new CladeStep(clade.slug(), clade.displayName(), CladeRanks.rankFor(clade));
    }
}
