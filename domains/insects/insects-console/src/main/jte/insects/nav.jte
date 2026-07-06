@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param CladeTrail cladeTrail = null

<div class="context-bar">
    @if(!breadcrumb.isEmpty())
        @template.insects.breadcrumb(segments = breadcrumb)
    @endif
    @if(cladeTrail != null)
        @template.insects.cladeTrail(trail = cladeTrail)
    @endif
</div>
