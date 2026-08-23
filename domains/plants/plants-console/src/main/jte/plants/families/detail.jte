@import com.naturalist.plants.PlantFamily
@import com.naturalist.plants.PlantGenusView
@import com.naturalist.plants.PlantTaxonView
@import java.util.List
@import com.naturalist.plants.BreadcrumbSegment
@import com.naturalist.clades.Clade
@import com.naturalist.plants.PlantFeatureView
@import com.naturalist.observation.OrganismImage
@import com.naturalist.plants.PlantImageId
@import com.naturalist.plants.PlantObservationId
@import com.naturalist.plants.PlantRankName

@param PlantFamily family
@param List<PlantTaxonView> children = java.util.List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<Clade> cladeTrail = java.util.List.of()
@param PlantFeatureView features = null
@param List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> images = java.util.List.of()

!{var commonName = family.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(family.family().value());}

@template.layout.page(title = family.family().value(), content = @`
    @template.plants.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    <h1>${commonName}</h1>
    <p><a href="/plants/orders/${family.orderName().value()}">${family.orderName().value()}</a>
        &middot; <em>${family.family().value()}</em></p>

    @template.components.description(
    preschool = descriptionPreschool,
    elementary = descriptionElementary,
    secondary = descriptionSecondary,
    university = descriptionUniversity)

    @template.plants.gallery(images = images, alt = commonName)

    @template.plants.features(features = features)

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Slug:</strong> <code>${family.name().value()}</code></li>
            <li><strong>Order:</strong>
                <a href="/plants/orders/${family.orderName().value()}">${family.orderName().value()}</a></li>
            <li><strong>Family:</strong> <em>${family.family().value()}</em></li>
            @if(!family.commonNames().isEmpty())
                <li>
                    <strong>Also known as:</strong>
                    @for(var cn : family.commonNames())
                        <span class="badge">${cn.label()}</span>
                    @endfor
                </li>
            @endif
        </ul>
    </section>

    <section>
        <h2>Genera in this family</h2>
        @if(children.isEmpty())
            <p><em>No genera catalogued under this family yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var child : children)
                    @if(child instanceof PlantGenusView gv)
                        !{var g = gv.genus();}
                        !{var genusCommonName = g.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(g.genus().value());}
                        <article>
                            <header>
                                <a href="/plants/genera/${g.name().value()}">
                                    <strong>${genusCommonName}</strong>
                                </a>
                            </header>
                            <p class="taxonomy"><em>${g.genus().value()}</em></p>
                            <p class="description-preview">${g.description().preschool()}</p>
                        </article>
                    @endif
                @endfor
            </div>
        @endif
    </section>
`)
