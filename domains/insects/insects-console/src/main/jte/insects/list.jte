@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.FunctionalGuild
@import com.naturalist.data.Page

@param Page<InsectSpecies> speciesPage

@template.layout.page(title = "Insect Catalog", content = @`
    <h1>Insect Catalog</h1>
    <p>Insect species documented at Oak Vista.</p>

    <div class="entity-grid">
        @for(var s : speciesPage.content())
            <article>
                <header>
                    <a href="/insects/${s.name().value()}">
                        <strong>${s.name().value()}</strong>
                    </a>
                    @if(s.beneficial())
                        <span class="badge beneficial">beneficial</span>
                    @endif
                </header>
                <p class="taxonomy">
                    ${s.taxonomy().order().value()} &middot; ${s.taxonomy().family().value()}
                    @if(s.taxonomy().genus() != null)
                        &middot; <em>${s.taxonomy().genus().value()}</em>
                    @endif
                </p>
                <div class="guilds">
                    @for(var guild : s.guilds())
                        <a href="/insects/guild/${guild.name()}"
                           class="badge guild-${guild.name().toLowerCase()}">${guild.name()}</a>
                    @endfor
                </div>
                <p class="description-preview">${s.description().preschool()}</p>
                <footer>
                    <a href="/insects/${s.name().value()}/life-stages" class="secondary">
                        Life Stages &rarr;
                    </a>
                </footer>
            </article>
        @endfor
    </div>

    @template.components.pager(page = speciesPage, baseUrl = "/insects")
`)
