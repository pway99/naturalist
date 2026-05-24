@import com.naturalist.insects.InsectGenus
@import com.naturalist.data.Page

@param Page<InsectGenus> genusPage

@template.layout.page(title = "Insect Genera", content = @`
    @template.insects.nav(active = "genus")
    <h1>Insect Genera</h1>
    <p>Genus-rank records in the Oak Vista catalog.</p>

    <div class="entity-grid">
        @for(var g : genusPage.content())
            !{var commonName = g.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(g.name().value());}
            <article>
                <header>
                    <a href="/insects/genera/${g.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>family</dt>
                    <dd><a href="/insects/families/${g.familyName().value()}">${g.family().value()}</a></dd>
                    <dt>genus</dt>
                    <dd><em>${g.genus().value()}</em></dd>
                </dl>
            </article>
        @endfor
    </div>

    @template.components.pager(page = genusPage, baseUrl = "/insects/genera")
`)
