@import com.naturalist.plants.PlantFamily
@import com.naturalist.plants.PlantGenus

@param PlantGenus genus
@param PlantFamily family = null
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

!{var commonName = genus.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(genus.genus().value());}

@template.layout.page(title = genus.genus().value(), content = @`
    <a href="/plants" class="naturalist-backlink">&larr; Back to catalog</a>

    <h1>${commonName}</h1>
    <p>
        ${genus.order().value()} &middot;
        <a href="/plants/families/${genus.familyName().value()}">${genus.family().value()}</a>
        &middot; <em>${genus.genus().value()}</em>
    </p>

    @template.components.description(
    preschool = descriptionPreschool,
    elementary = descriptionElementary,
    secondary = descriptionSecondary,
    university = descriptionUniversity)

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Slug:</strong> <code>${genus.name().value()}</code></li>
            <li><strong>Order:</strong> ${genus.order().value()}</li>
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

    <%-- The genus → plants rollup needs PlantSpecies.genusName, which does not exist
         yet. See M2b/M2f in docs/plans/2026-08-15-plants-domain-consistency-plan.md;
         this section becomes a card grid once the typed FK lands. --%>
    <section>
        <h2>Plants in this genus</h2>
        <p><em>Species-level records are not yet linked to their genus.</em></p>
    </section>
`)
