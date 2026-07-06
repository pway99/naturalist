@import com.naturalist.library.CladeQuery.CladeTreeNode
@import com.naturalist.library.CladeView

@param CladeTreeNode root
@param CladeView rootView = null

@template.layout.page(title = "Tree of Life", content = @`
    @if(rootView != null)
        <div class="context-bar">
            <nav class="breadcrumb-trail" aria-label="Phylogenetic lineage">
                <span class="breadcrumb-segment">
                    <span class="breadcrumb-current"><strong>${rootView.subject().displayName()}</strong></span>
                </span>
            </nav>
        </div>
    @endif

    <h1>Tree of Life</h1>
    <p>Every clade in the catalog, from Eukaryota down.
       <a href="/concepts/clade">What is a clade?</a></p>
    <ul class="clade-tree">
        @template.clades.node(node = root)
    </ul>
`)
