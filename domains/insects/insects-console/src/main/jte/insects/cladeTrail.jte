@import com.naturalist.insects.console.CladeTrail
@import com.naturalist.insects.console.CladeNav
@import com.naturalist.clades.Clade
@import com.naturalist.clades.CladeCatalog

@param CladeTrail trail

<%-- The phylogenetic tree-of-life row: a lineage of clade links. Every node links to its
     in-console clade page (routed by CladeNav.pageUrl — insect/animal clades to
     /insects/clades, plant clades to /plants/clades), so the row never strands the
     naturalist in the shared browser. On hover each node opens a dropdown of its narrower
     (child) clades for quick drilling; the clade pages also list those clades. --%>
<nav class="clade-trail" aria-label="Phylogenetic lineage">
    <span class="clade-trail-label">Tree of life</span>
    @for(int i = 0; i < trail.steps().size(); i++)
        @if(i > 0)
            <span class="clade-trail-sep" aria-hidden="true">›</span>
        @endif
        !{var step = trail.steps().get(i);}
        !{var node = Clade.of(step.cladeSlug());}
        !{var narrower = CladeCatalog.childrenOf(node);}
        <span class="clade-node">
            <a class="clade-trail-node" href="${CladeNav.pageUrl(node)}">${step.displayName()}</a>
            @if(!narrower.isEmpty())
                <span class="clade-node-caret" aria-hidden="true">▾</span>
                <span class="clade-menu" role="menu">
                    @for(var child : narrower)
                        <a role="menuitem" href="${CladeNav.pageUrl(child)}">${child.displayName()}</a>
                    @endfor
                </span>
            @endif
        </span>
    @endfor
    @if(trail.hasGap())
        <span class="clade-trail-sep" aria-hidden="true">·</span>
        <a class="clade-trail-gap" href="/concepts/placing-clades"
           title="This lineage isn't placed in the tree of life yet — research and help map it">
            ${trail.gapLabel()} — not yet placed
        </a>
    @endif
    @template.components.infoPopover(
        id = "clade-trail-help",
        label = "What is this Tree of Life row?",
        text = "This row is the <strong>phylogenetic tree of life</strong> — the actual evolutionary"
            + " branches, shown parallel to the Linnaean rank ladder. Each name opens that clade's page"
            + " (the insects in it, grouped by rank, and its narrower clades); hover to drill straight"
            + " into a narrower clade. Most branches line up with a rank, but some (like"
            + " <em>Holometabola</em>) sit between ranks with no rank name of their own.",
        moreHref = "/concepts/clade-taxonomy-relation",
        moreLabel = "More on clades vs. taxonomy →")
</nav>
