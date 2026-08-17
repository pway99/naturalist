@import com.naturalist.clades.Clade
@import com.naturalist.plants.console.PlantCladeTree
@import java.util.List

@param List<Clade> cladeTrail = java.util.List.of()
@param Clade current = null

<%-- The phylogenetic tree-of-life row: a lineage of clade links, distinct from the
     taxonomic rank breadcrumb. Every node links to its in-console clade page (routed by
     PlantCladeTree.pageUrl — plant clades to /plants/clades, animal clades to
     /insects/clades, the shared Eukaryota root to the current domain), so the row never
     strands the naturalist in the shared tree-of-life browser. On hover or keyboard focus
     each node opens a dropdown of its narrower (child) clades for quick drilling; the
     child pages also list those clades. The row runs up to Eukaryota, the shared root
     where the kingdoms meet — hovering it surfaces Animalia, the crossover to the insects. --%>
@if(!cladeTrail.isEmpty())
    <nav class="clade-trail" aria-label="Phylogenetic lineage">
        <span class="clade-trail-label">Tree of life</span>
        @for(int i = 0; i < cladeTrail.size(); i++)
            @if(i > 0)
                <span class="clade-trail-sep" aria-hidden="true">›</span>
            @endif
            !{var node = cladeTrail.get(i);}
            !{var narrower = PlantCladeTree.narrower(node);}
            <span class="clade-node">
                @if(current != null && node.slug().equals(current.slug()))
                    <span class="clade-trail-node clade-trail-current"><strong>${node.displayName()}</strong></span>
                @else
                    <a class="clade-trail-node" href="${PlantCladeTree.pageUrl(node)}">${node.displayName()}</a>
                @endif
                @if(!narrower.isEmpty())
                    <span class="clade-node-caret" aria-hidden="true">▾</span>
                    <span class="clade-menu" role="menu">
                        @for(var child : narrower)
                            <a role="menuitem" href="${PlantCladeTree.pageUrl(child)}">${child.displayName()}</a>
                        @endfor
                    </span>
                @endif
            </span>
        @endfor
        @template.components.infoPopover(
            id = "clade-trail-help",
            label = "What is this Tree of Life row?",
            text = "This row is the <strong>phylogenetic tree of life</strong> — the real evolutionary"
                + " branches — shown parallel to the Linnaean rank ladder. Each name opens that clade's"
                + " page (its organisms and narrower clades); hover to drill straight into a narrower"
                + " clade. It runs up to <em>Eukaryota</em>, the shared root where plants and animals"
                + " meet, so you can cross to the insect side. In plants, every branch between"
                + " <em>Kingdom</em> and <em>Order</em> is a rank-free clade sitting where Phylum and"
                + " Class would be — which is why a plant order has no Class rank above it.",
            moreHref = "/concepts/class",
            moreLabel = "What is a class? →")
    </nav>
@endif
