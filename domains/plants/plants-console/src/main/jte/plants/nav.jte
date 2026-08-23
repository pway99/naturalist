@import com.naturalist.plants.BreadcrumbSegment
@import com.naturalist.clades.Clade
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<Clade> cladeTrail = java.util.List.of()
@param Clade current = null

<%-- Two-row context bar mirroring insects/nav.jte: the phylogenetic tree-of-life
     row (cladeTrail) on top as the primary, cross-domain clade navigator, and the
     taxonomic rank ladder (breadcrumb) below it. Plant clades attach at the order,
     so the clade row is populated once an order carries a placedIn clade. --%>
<div class="context-bar">
    @if(!cladeTrail.isEmpty())
        @template.plants.cladeTrail(cladeTrail = cladeTrail, current = current)
    @endif
    @if(!breadcrumb.isEmpty())
        @template.plants.breadcrumb(segments = breadcrumb)
    @endif
</div>
