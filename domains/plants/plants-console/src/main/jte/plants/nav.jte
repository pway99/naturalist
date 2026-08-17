@import com.naturalist.plants.console.BreadcrumbSegment
@import com.naturalist.clades.Clade
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<Clade> cladeTrail = java.util.List.of()
@param Clade current = null

<%-- Two-row context bar mirroring insects/nav.jte: the taxonomic rank ladder
     (breadcrumb) and, below it, the phylogenetic tree-of-life row (cladeTrail).
     Plant clades attach at the order, so the clade row is populated on order
     pages once an order carries a placedIn clade. --%>
<div class="context-bar">
    @if(!breadcrumb.isEmpty())
        @template.plants.breadcrumb(segments = breadcrumb)
    @endif
    @if(!cladeTrail.isEmpty())
        @template.plants.cladeTrail(cladeTrail = cladeTrail, current = current)
    @endif
</div>
