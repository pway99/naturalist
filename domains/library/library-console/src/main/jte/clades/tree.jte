@import com.naturalist.library.console.clade.CladesController.CladeNode
@param CladeNode root

@template.layout.page(title = "Tree of Life", content = @`
    <h1>Tree of Life</h1>
    <p>Every clade in the catalog, from Eukaryota down.
       <a href="/concepts/clade">What is a clade?</a></p>
    <ul class="clade-tree">
        @template.clades.node(node = root)
    </ul>
`)
