@import com.naturalist.insects.console.CladeTaxonCard
@import java.util.List

@param String heading
@param List<CladeTaxonCard> taxa = java.util.List.of()

<%-- One rank's worth of insect taxa placed directly at a clade (Orders / Families /
     Genera / Species), rendered as a card grid. Omitted entirely when the rank is empty. --%>
@if(!taxa.isEmpty())
    <section>
        <h2>${heading}</h2>
        <div class="entity-grid">
            @for(var t : taxa)
                <article>
                    <header>
                        <a href="${t.url()}"><strong>${t.name()}</strong></a>
                    </header>
                </article>
            @endfor
        </div>
    </section>
@endif
