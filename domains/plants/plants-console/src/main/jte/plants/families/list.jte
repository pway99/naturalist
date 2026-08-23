@import com.naturalist.data.Page
@import com.naturalist.plants.PlantFamily
@import com.naturalist.plants.BreadcrumbSegment
@import com.naturalist.clades.Clade
@import java.util.List

@param Page<PlantFamily> familiesPage
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<Clade> cladeTrail = java.util.List.of()

@template.layout.page(title = "Plant Families", content = @`
    @template.plants.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
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
