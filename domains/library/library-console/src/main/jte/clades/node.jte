@import com.naturalist.library.CladeQuery.CladeTreeNode
@import com.naturalist.library.clade.CladeUrls
@param CladeTreeNode node

<li>
    <a href="${CladeUrls.of(node.slug())}">${node.displayName()}</a>
    @if(!node.children().isEmpty())
        <ul>
            @for(var child : node.children())
                @template.clades.node(node = child)
            @endfor
        </ul>
    @endif
</li>
