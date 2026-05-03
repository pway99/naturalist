@import com.naturalist.console.search.SearchController.Group
@import com.naturalist.console.search.SearchController.Link
@import java.util.List

@param String query
@param List<Group> groups
@param int totalHits

@template.layout.page(title = "Search", query = query, content = @`
    <section class="search-page">
        <h1>Search</h1>

        @if(query.isBlank())
            <p>Type a name, a genus, or a common term to discover what the catalog knows.</p>
        @elseif(totalHits == 0)
            <div class="search-empty-state">
                <h2>The catalog doesn't know <em>${query}</em> yet.</h2>
                <p>If this is something you've observed in the field, add it to your collection &mdash;
                    a future naturalist will thank you.</p>
                <p>
                    <button type="button" disabled title="Personal collections coming soon">
                        Add to my collection
                    </button>
                </p>
            </div>
        @else
            <p>${totalHits} hit${totalHits == 1 ? "" : "s"} for <strong>${query}</strong>.</p>

            @for(var group : groups)
                <div class="search-domain-group">
                    <h2>${group.domainDisplayName()}</h2>
                    <ul>
                        @for(var link : group.links())
                            <li>
                                <a href="${link.url()}">${link.label()}</a>
                                @if(link.matchedHint() != null)
                                    <small class="search-matched-hint">matched: ${link.matchedHint()}</small>
                                @endif
                            </li>
                        @endfor
                    </ul>
                </div>
            @endfor
        @endif
    </section>
`)
