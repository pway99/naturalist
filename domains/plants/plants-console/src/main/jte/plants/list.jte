@import com.naturalist.plants.Plant
@import com.naturalist.data.Page

@param Page<Plant> plantsPage

@template.layout.page(title = "Plant Catalog", content = @`
    <h1>Plant Catalog</h1>
    <p>Plants documented at Oak Vista. <a href="/plants/phytochemistry">View phytochemistry &rarr;</a></p>

    <div class="entity-grid">
        @for(var p : plantsPage.content())
            <article>
                <header>
                    <a href="/plants/${p.name().value()}">
                        <strong>${p.name().value()}</strong>
                    </a>
                    @if(p.isKeystoneHost())
                        <span class="badge guild-apex_predator">keystone host</span>
                    @endif
                    @if(p.isNitrogenFixer())
                        <span class="badge guild-pollinator">nitrogen fixer</span>
                    @endif
                    @if(p.supportsBiocontrolInsects())
                        <span class="badge beneficial">beneficial insect habitat</span>
                    @endif
                </header>
                <p class="taxonomy">
                    ${p.taxonomy().order().value()} &middot; ${p.taxonomy().family().value()}
                    @if(p.taxonomy().genus() != null)
                        &middot; <em>${p.taxonomy().genus().value()}</em>
                    @endif
                    @if(p.taxonomy().species() != null)
                        ${p.taxonomy().species().value()}
                    @endif
                </p>
                <p class="description-preview">
                    <strong>Life form:</strong> ${p.lifeForm().name()}<br>
                    <strong>Roles:</strong>
                    @for(var role : p.roles())
                        <span class="badge">${role.name()}</span>
                    @endfor
                </p>
                <p class="description-preview">${p.description().preschool()}</p>
                <footer>
                    <a href="/plants/${p.name().value()}">
                        Plant details &rarr;
                    </a>
                </footer>
            </article>
        @endfor
    </div>

    @template.components.pager(page = plantsPage, baseUrl = "/plants")
`)
