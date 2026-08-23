@import com.naturalist.plants.PlantFamily
@import com.naturalist.plants.PlantGenus
@import com.naturalist.plants.PlantSpeciesView
@import com.naturalist.plants.PlantTaxonView
@import com.naturalist.plants.BreadcrumbSegment
@import com.naturalist.clades.Clade
@import com.naturalist.plants.PlantFeatureView
@import com.naturalist.observation.OrganismImage
@import com.naturalist.plants.PlantImageId
@import com.naturalist.plants.PlantObservationId
@import com.naturalist.plants.PlantRankName
@import java.util.List

@param PlantGenus genus
@param PlantFamily family = null
@param List<PlantTaxonView> children = java.util.List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<Clade> cladeTrail = java.util.List.of()
@param PlantFeatureView features = null
@param List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> images = java.util.List.of()

!{var commonName = genus.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(genus.genus().value());}

@template.layout.page(title = genus.genus().value(), content = @`
    @template.plants.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    <h1>${commonName}</h1>
    <p>
        <a href="/plants/families/${genus.familyName().value()}">${genus.family().value()}</a>
        &middot; <em>${genus.genus().value()}</em>
    </p>

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
            <li><strong>Slug:</strong> <code>${genus.name().value()}</code></li>
            <li>
                <strong>Family:</strong>
                <a href="/plants/families/${genus.familyName().value()}">
                    <em>${genus.family().value()}</em>
                </a>
                @if(family != null && !family.commonNames().isEmpty())
                    &middot; ${family.commonNames().stream().findFirst().map(cn -> cn.label()).orElse("")}
                @endif
            </li>
            <li><strong>Genus:</strong> <em>${genus.genus().value()}</em></li>
            @if(!genus.commonNames().isEmpty())
                <li>
                    <strong>Also known as:</strong>
                    @for(var cn : genus.commonNames())
                        <span class="badge">${cn.label()}</span>
                    @endfor
                </li>
            @endif
        </ul>
    </section>

    <section>
        <h2>Species in this genus</h2>
        @if(children.isEmpty())
            <p><em>No species catalogued under this genus yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var child : children)
                    @if(child instanceof PlantSpeciesView sv)
                        !{var s = sv.species();}
                        !{var speciesLabel = s.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(genus.genus().value() + " " + s.epithet().value());}
                        <article>
                            <header>
                                <a href="/plants/${s.name().value()}">
                                    <strong>${speciesLabel}</strong>
                                </a>
                            </header>
                            <p class="taxonomy"><em>${genus.genus().value()} ${s.epithet().value()}</em></p>
                            <p class="description-preview">${s.description().preschool()}</p>
                        </article>
                    @endif
                @endfor
            </div>
        @endif
    </section>
`)
