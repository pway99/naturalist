@import com.naturalist.insects.InsectFamily
@import com.naturalist.data.Page

@param Page<InsectFamily> familyPage

@template.layout.page(title = "Insect Families", content = @`
    <a href="/insects">&larr; Back to species catalog</a>
    <h1>Insect Families</h1>
    <p>Family-rank records in the Oak Vista catalog.
       <a href="/insects/genera">Browse genera &rarr;</a></p>

    <div class="entity-grid">
        @for(var f : familyPage.content())
            !{var commonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.name().value());}
            <article>
                <header>
                    <a href="/insects/families/${f.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>order</dt>
                    <dd>${f.order().value()}</dd>
                    <dt>family</dt>
                    <dd><em>${f.family().value()}</em></dd>
                </dl>
            </article>
        @endfor
    </div>

    @template.components.pager(page = familyPage, baseUrl = "/insects/families")
`)
