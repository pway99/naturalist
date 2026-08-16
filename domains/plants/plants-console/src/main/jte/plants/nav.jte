@import com.naturalist.plants.console.BreadcrumbSegment
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()

<%-- Context bar mirroring insects/nav.jte. Plants carry no clade in the kernel
     yet, so this holds only the taxonomic breadcrumb; the clade trail row that
     the insects console adds has no plant equivalent to render. --%>
<div class="context-bar">
    @if(!breadcrumb.isEmpty())
        @template.plants.breadcrumb(segments = breadcrumb)
    @endif
</div>
