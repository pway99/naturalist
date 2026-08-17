@import com.naturalist.clades.Clade
@import com.naturalist.plants.console.PlantCladeTree
@import java.util.List

@param List<Clade> cladeTrail = java.util.List.of()
@param Clade current = null

<%-- The phylogenetic row and the plant catalog's clade navigator, distinct from the
     taxonomic rank breadcrumb. Plant clades link to their in-console page
     (/plants/clades/{slug}, showing the orders placed there); on hover or keyboard focus
     each opens a dropdown of its narrower (child) clades, so the whole clade tree is
     walkable inline. Siblings need no dropdown — they are the parent node's children,
     reached from the node one step to the left. Plant clades are all supra-ordinal, so
     everything between Kingdom (Plantae) and Order is a rank-free clade. The row runs up
     to Eukaryota, the shared root where the plant and animal kingdoms meet: its node
     links to the cross-domain tree-of-life browser and its dropdown reaches Animalia, so
     a naturalist can cross from plants to the insect side and back. --%>
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
                + " branches — and the plant catalog's clade navigator. Hover a name to drill into a"
                + " narrower clade; click it to open its page. It runs up to <em>Eukaryota</em>, the"
                + " shared root where plants and animals meet, so you can cross to the insect side there."
                + " In plants, every branch between"
                + " <em>Kingdom</em> and <em>Order</em> is a <strong>rank-free clade</strong> sitting where"
                + " Phylum and Class would be, which is why a plant order has no Class rank above it.",
            moreHref = "/concepts/class",
            moreLabel = "What is a class? →")
    </nav>
@endif
