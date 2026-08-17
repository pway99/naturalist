@import com.naturalist.clades.Clade
@import java.util.List

@param List<Clade> cladeTrail = java.util.List.of()

<%-- The phylogenetic row: the real evolutionary branches from the plant kingdom
     (Plantae) down to the order's clade, parallel to the taxonomic rank ladder above.
     Plant clades are all supra-ordinal, so everything between Kingdom (Plantae) and the
     Order is a rank-free clade — which is why a plant order has no Class above it.
     Eukaryota, the shared root above Plantae, is omitted to keep the row in the plant
     world; it is one click up from the Plantae node. --%>
@if(!cladeTrail.isEmpty())
    <nav class="clade-trail" aria-label="Phylogenetic lineage">
        <span class="clade-trail-label">Tree of life</span>
        @for(int i = 0; i < cladeTrail.size(); i++)
            @if(i > 0)
                <span class="clade-trail-sep" aria-hidden="true">›</span>
            @endif
            !{var node = cladeTrail.get(i);}
            <a class="clade-trail-node" href="/clades/${node.slug()}">${node.displayName()}</a>
        @endfor
        @template.components.infoPopover(
            id = "clade-trail-help",
            label = "What is this Tree of Life row?",
            text = "This row is the <strong>phylogenetic tree of life</strong> — the real evolutionary"
                + " branches — shown parallel to the Linnaean rank ladder above. In plants, every branch"
                + " between <em>Kingdom</em> and <em>Order</em> (angiosperms, monocots, eudicots, the rosid"
                + " and asterid groups) is a <strong>rank-free clade</strong>: a genuine lineage sitting"
                + " where Phylum and Class would be, but carrying no Linnaean rank of its own. That is why"
                + " a plant order has no Class rank above it.",
            moreHref = "/concepts/class",
            moreLabel = "What is a class? →")
    </nav>
@endif
