@import com.naturalist.library.CladeQuery.CladeTreeNode
@import com.naturalist.library.CladeView

@param CladeTreeNode root
@param CladeView rootView = null

@template.layout.page(title = "Tree of Life", content = @`
    @if(rootView != null)
        <nav class="dual-breadcrumb" aria-label="Clade navigation">
            <span class="dual-breadcrumb-clade"><a href="/concepts/clade">Phylogenetic lineage</a></span>
            <span class="dual-breadcrumb-rank"><a href="/concepts/taxonomic-rank">Linnaean rank ladder</a></span>

            <span class="dual-breadcrumb-clade dual-breadcrumb-sep" aria-hidden="true">›</span>
            <span class="dual-breadcrumb-rank dual-breadcrumb-spacer"></span>

            <span class="dual-breadcrumb-clade dual-breadcrumb-current"><strong>${rootView.subject().displayName()}</strong></span>
            <span class="dual-breadcrumb-rank dual-breadcrumb-gap" aria-label="no rank">—</span>
        </nav>
    @endif

    <h1>Tree of Life</h1>
    <p>Every clade in the catalog, from Eukaryota down.
       <a href="/concepts/clade">What is a clade?</a></p>
    <ul class="clade-tree">
        @template.clades.node(node = root)
    </ul>
`)
