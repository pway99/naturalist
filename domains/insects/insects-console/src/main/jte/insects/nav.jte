@import com.naturalist.insects.BreadcrumbSegment
@import com.naturalist.insects.CladeTrail
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param CladeTrail cladeTrail = null

<div class="context-bar">
    @if(cladeTrail != null)
        @template.insects.cladeTrail(trail = cladeTrail)
    @endif
    @if(!breadcrumb.isEmpty())
        @template.insects.breadcrumb(segments = breadcrumb)
    @endif
</div>
