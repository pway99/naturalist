@import com.naturalist.plants.PlantFamily
@import com.naturalist.plants.PlantOrder
@import java.util.List
@import com.naturalist.plants.console.BreadcrumbSegment
@import com.naturalist.clades.Clade
@import com.naturalist.plants.PlantFeatureView

@param PlantOrder order
@param List<PlantFamily> families = java.util.List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<Clade> cladeTrail = java.util.List.of()
@param PlantFeatureView features = null

!{var commonName = order.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(order.order().value());}

@template.layout.page(title = order.order().value(), content = @`
    @template.plants.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    <h1>${commonName}</h1>
    <p><em>${order.order().value()}</em></p>

    @template.components.description(
    preschool = descriptionPreschool,
    elementary = descriptionElementary,
    secondary = descriptionSecondary,
    university = descriptionUniversity)

    @template.plants.features(features = features)

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Slug:</strong> <code>${order.name().value()}</code></li>
            <li><strong>Order:</strong> <em>${order.order().value()}</em></li>
            @if(!order.commonNames().isEmpty())
                <li>
                    <strong>Also known as:</strong>
                    @for(var cn : order.commonNames())
                        <span class="badge">${cn.label()}</span>
                    @endfor
                </li>
            @endif
        </ul>
    </section>

    <section>
        <h2>Families in this order</h2>
        @if(families.isEmpty())
            <p><em>No families catalogued under this order yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var f : families)
                    !{var familyCommonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.family().value());}
                    <article>
                        <header>
                            <a href="/plants/families/${f.name().value()}">
                                <strong>${familyCommonName}</strong>
                            </a>
                        </header>
                        <p class="taxonomy"><em>${f.family().value()}</em></p>
                        <p class="description-preview">${f.description().preschool()}</p>
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
