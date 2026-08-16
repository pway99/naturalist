@import com.naturalist.data.Page
@import com.naturalist.plants.PlantFamily

@param Page<PlantFamily> familiesPage

@template.layout.page(title = "Plant Families", content = @`
    <a href="/plants" class="naturalist-backlink">&larr; Back to catalog</a>

    <h1>Plant Families</h1>
    <p>
        Family-rank records are first-class catalog citizens, not placeholders — a
        herb recognised as Lamiaceae without a resolved genus has a permanent home here.
    </p>

    <div class="entity-grid">
        @for(var f : familiesPage.content())
            !{var commonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.family().value());}
            <article>
                <header>
                    <a href="/plants/families/${f.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <p class="taxonomy">${f.orderName().value()} &middot; <em>${f.family().value()}</em></p>
                <p class="description-preview">${f.description().preschool()}</p>
                <footer>
                    <a href="/plants/families/${f.name().value()}">
                        Family details &rarr;
                    </a>
                </footer>
            </article>
        @endfor
    </div>

    @template.components.pager(page = familiesPage, baseUrl = "/plants/families")
`)
