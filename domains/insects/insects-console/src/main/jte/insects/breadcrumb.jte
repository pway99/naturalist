@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param List<BreadcrumbSegment> segments

<nav class="breadcrumb-trail" aria-label="Taxonomic placement">
    @for(int i = 0; i < segments.size(); i++)
        @if(i > 0)
            <span class="breadcrumb-sep" aria-hidden="true">›</span>
        @endif
        !{var s = segments.get(i);}
        <span class="breadcrumb-segment">
            @if(s.rank() != null)
                <span class="breadcrumb-rank">${s.rank()}</span>
            @endif
            @if(s.currentPage())
                <span class="breadcrumb-current"><strong>${s.label()}</strong></span>
            @elseif(s.url() != null)
                <a href="${s.url()}">${s.label()}</a>
            @else
                <span class="breadcrumb-name">${s.label()}</span>
            @endif
        </span>
    @endfor
</nav>
