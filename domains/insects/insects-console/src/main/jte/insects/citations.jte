@import com.naturalist.insects.InsectCitationView
@import com.naturalist.authority.OnlineSource

@param InsectCitationView citations = null

@if(citations != null && !citations.citations().isEmpty())
    <section class="citations">
        <h2>Citations</h2>
        <ul class="citation-list">
            @for(var rc : citations.citations())
                <li>
                    <a href="${rc.citation().authorityReference().url().toString()}"
                       target="naturalist-citation">${rc.citation().title()}</a>
                    @if(rc.citation() instanceof OnlineSource os)
                        @if(os.author() != null || os.year() != null)
                            <span class="citation-meta">
                                — @if(os.author() != null)${os.author()}@endif@if(os.author() != null && os.year() != null), @endif@if(os.year() != null)${os.year()}@endif
                            </span>
                        @endif
                    @endif
                    <small class="citation-source">(${rc.attachedAt().value()})</small>
                    @if(rc.note() != null)
                        <span class="citation-note">— ${rc.note()}</span>
                    @endif
                </li>
            @endfor
        </ul>
    </section>
@endif
