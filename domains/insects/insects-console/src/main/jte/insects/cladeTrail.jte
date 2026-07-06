@import com.naturalist.insects.console.CladeTrail

@param CladeTrail trail

<nav class="clade-trail" aria-label="Phylogenetic lineage">
    <span class="clade-trail-label">Tree of life</span>
    @for(int i = 0; i < trail.steps().size(); i++)
        @if(i > 0)
            <span class="clade-trail-sep" aria-hidden="true">›</span>
        @endif
        !{var step = trail.steps().get(i);}
        <a class="clade-trail-node" href="/clades/${step.cladeSlug()}">${step.displayName()}</a>
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
            + " branches. It runs parallel to the taxonomic breadcrumb above: most branches line up with"
            + " a Linnaean rank, but some (like <em>Holometabola</em>) sit between ranks with no rank name"
            + " of their own.",
        moreHref = "/concepts/clade-taxonomy-relation",
        moreLabel = "More on clades vs. taxonomy →")
</nav>
