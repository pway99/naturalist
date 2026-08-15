@import com.naturalist.plants.PlantFamily
@import com.naturalist.plants.PlantGenus
@import java.util.List

@param PlantFamily family
@param List<PlantGenus> genera = java.util.List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

!{var commonName = family.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(family.family().value());}

@template.layout.page(title = family.family().value(), content = @`
    <a href="/plants" class="naturalist-backlink">&larr; Back to catalog</a>

    <h1>${commonName}</h1>
    <p>${family.order().value()} &middot; <em>${family.family().value()}</em></p>

    @template.components.description(
    preschool = descriptionPreschool,
    elementary = descriptionElementary,
    secondary = descriptionSecondary,
    university = descriptionUniversity)

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Slug:</strong> <code>${family.name().value()}</code></li>
            <li><strong>Order:</strong> ${family.order().value()}</li>
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
        @if(genera.isEmpty())
            <p><em>No genera catalogued under this family yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var g : genera)
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
                @endfor
            </div>
        @endif
    </section>
`)
