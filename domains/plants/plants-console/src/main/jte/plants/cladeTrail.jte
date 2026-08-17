@import com.naturalist.clades.Clade
@import java.util.List

@param List<Clade> cladeTrail = java.util.List.of()
@param Clade current = null

<%-- The phylogenetic row: the real evolutionary branches from the plant kingdom
     (Plantae) down to the clade in view. Each node drives a plant query — it links
     to that clade's page in the plant catalog (/plants/clades/{slug}), which shows
     the orders placed in it — so the row navigates within the plants console rather
     than leaving for the shared tree-of-life browser. Plant clades are all
     supra-ordinal, so everything between Kingdom (Plantae) and the Order is a
     rank-free clade — which is why a plant order has no Class above it. Eukaryota,
     the shared root above Plantae, is omitted to keep the row in the plant world. --%>
@if(!cladeTrail.isEmpty())
    <nav class="clade-trail" aria-label="Phylogenetic lineage">
        <span class="clade-trail-label">Tree of life</span>
        @for(int i = 0; i < cladeTrail.size(); i++)
            @if(i > 0)
                <span class="clade-trail-sep" aria-hidden="true">›</span>
            @endif
            !{var node = cladeTrail.get(i);}
            @if(current != null && node.slug().equals(current.slug()))
                <span class="clade-trail-node clade-trail-current"><strong>${node.displayName()}</strong></span>
            @else
                <a class="clade-trail-node" href="/plants/clades/${node.slug()}">${node.displayName()}</a>
            @endif
        @endfor
        @template.components.infoPopover(
            id = "clade-trail-help",
            label = "What is this Tree of Life row?",
            text = "This row is the <strong>phylogenetic tree of life</strong> — the real evolutionary"
                + " branches — shown parallel to the Linnaean rank ladder above. Each name links to the"
                + " plant orders placed in that clade. In plants, every branch between <em>Kingdom</em> and"
                + " <em>Order</em> (angiosperms, monocots, eudicots, the rosid and asterid groups) is a"
                + " <strong>rank-free clade</strong>: a genuine lineage sitting where Phylum and Class would"
                + " be, but carrying no Linnaean rank of its own. That is why a plant order has no Class"
                + " rank above it.",
            moreHref = "/concepts/class",
            moreLabel = "What is a class? →")
    </nav>
@endif
