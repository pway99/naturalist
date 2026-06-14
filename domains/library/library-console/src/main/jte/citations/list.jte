@import com.naturalist.authority.Citation
@import com.naturalist.authority.OnlineSource
@import java.util.List
@param List<Citation> citations

@template.layout.page(title = "Citations", content = @`
    <h1>Citations</h1>
    <p>External authority references used across the catalog.</p>
    <ul class="citation-list">
        @for(var citation : citations)
            <li>
                <a href="${citation.authorityReference().url().toString()}" target="naturalist-citation"
                   rel="noopener">${citation.title()}</a>
                @if(citation instanceof OnlineSource os)
                    @if(os.author() != null || os.year() != null)
                        <span class="citation-meta">
                            — @if(os.author() != null)${os.author()}@endif@if(os.author() != null && os.year() != null), @endif@if(os.year() != null)${os.year()}@endif
                        </span>
                    @endif
                @endif
                <small class="citation-source">(${citation.authorityReference().source().displayName()})</small>
            </li>
        @endfor
    </ul>
`)
