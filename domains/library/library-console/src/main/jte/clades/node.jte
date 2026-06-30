@import com.naturalist.library.CladeQuery.CladeTreeNode
@param CladeTreeNode node

<li>
    <a href="/clades/${node.slug()}">${node.displayName()}</a>
    @if(!node.children().isEmpty())
        <ul>
            @for(var child : node.children())
                @template.clades.node(node = child)
            @endfor
        </ul>
    @endif
</li>
