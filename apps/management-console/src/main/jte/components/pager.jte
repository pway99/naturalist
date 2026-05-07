@import com.naturalist.data.Page

@param Page<?> page
@param String baseUrl

@if(page.hasNext() || page.pageNumber() > 0)
    <nav class="pager">
        <span class="pager-status">
            Page ${page.pageNumber() + 1}@if(page.pagesAheadKnown() > 0) of ${page.pageNumber() + 1 + page.pagesAheadKnown()}@if(page.moreBeyondLookahead())+@endif@endif
        </span>
        @if(page.pageNumber() > 0)
            <a class="pager-prev" href="${baseUrl}?page=${page.pageNumber() - 1}">&larr; Previous</a>
        @endif
        @if(page.hasNext())
            <a class="pager-next" href="${baseUrl}?page=${page.pageNumber() + 1}">Next &rarr;</a>
        @endif
        @if(page.moreBeyondLookahead())
            <span class="pager-hint">More results &mdash; narrow your search to find what you're looking for.</span>
        @endif
    </nav>
@endif
