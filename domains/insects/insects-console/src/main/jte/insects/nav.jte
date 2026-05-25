@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param String active = ""
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()

<div class="insects-context-bar">
    <nav class="insects-subnav" aria-label="Insects catalog sub-navigation">
        <a href="/insects/orders" aria-current="${"order".equals(active) ? "page" : "false"}">Orders</a>
        <a href="/insects/families" aria-current="${"family".equals(active) ? "page" : "false"}">Families</a>
        <a href="/insects/genera" aria-current="${"genus".equals(active) ? "page" : "false"}">Genera</a>
        <a href="/insects/species" aria-current="${"species".equals(active) ? "page" : "false"}">Species</a>
    </nav>
    @if(!breadcrumb.isEmpty())
        @template.insects.breadcrumb(segments = breadcrumb)
    @endif
</div>
