@import com.naturalist.insects.BreadcrumbSegment
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
    @template.components.infoPopover(
        id = "rank-ladder-help",
        label = "What is this row of ranks?",
        text = "This is the <strong>Linnaean rank ladder</strong> — the nested ranks every organism is"
            + " filed under, from Kingdom down through Phylum, Class, Order, Family and Genus to Species."
            + " The small label above each name is its rank. The lower row traces the same lineage as the"
            + " tree of life.",
        moreHref = "/concepts/taxonomic-rank",
        moreLabel = "More on the rank ladder →")
</nav>
