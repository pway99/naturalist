@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()

<div class="insects-context-bar">
    @if(!breadcrumb.isEmpty())
        @template.insects.breadcrumb(segments = breadcrumb)
    @endif
</div>
