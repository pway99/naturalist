@import com.naturalist.plants.PlantEcologicalRole
@import com.naturalist.plants.PlantSpecies
@import java.util.Map
@import com.naturalist.data.Page
@import com.naturalist.plants.console.BreadcrumbSegment
@import java.util.List

@param Page<PlantSpecies> plantsPage
@param Map<String, PlantEcologicalRole> ecologicalRoles = java.util.Map.of()
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()

@template.layout.page(title = "Plant Species", content = @`
    @template.plants.nav(breadcrumb = breadcrumb)
    <h1>Plant Species</h1>
    <p>
        Every species documented at Oak Vista.
        <a href="/plants/families">Browse families &rarr;</a>
        &middot;
        <a href="/plants/phytochemistry">View phytochemistry &rarr;</a>
    </p>

    <div class="entity-grid">
        @for(var p : plantsPage.content())
            <article>
                <header>
                    <a href="/plants/${p.name().value()}">
                        <strong>${p.name().value()}</strong>
                    </a>
                    !{var role = ecologicalRoles.get(p.name().value());}
                    @if(role != null && role.isKeystoneHost())
                        <span class="badge guild-apex_predator">keystone host</span>
                    @endif
                    @if(role != null && role.isNitrogenFixer())
                        <span class="badge guild-pollinator">nitrogen fixer</span>
                    @endif
                    @if(role != null && role.supportsBiocontrolInsects())
                        <span class="badge beneficial">beneficial insect habitat</span>
                    @endif
                </header>
                <p class="taxonomy">
                    ${p.genusName().value()} &middot; <em>${p.epithet().value()}</em>
                </p>
                <p class="description-preview">
                    <strong>Life form:</strong> ${p.lifeForm().name()}<br>
                    <strong>Roles:</strong>
                    @if(role == null)
                        <span class="not-reported">not characterised</span>
                    @else
                        @for(var r : role.roles())
                            <span class="badge">${r.name()}</span>
                        @endfor
                    @endif
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
