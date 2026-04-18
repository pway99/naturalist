@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.InsectImage
@import java.util.List

@param InsectSpecies species
@param List<InsectImage> images

@template.layout.page(title = species.name().value(), content = @`
    <a href="/insects">&larr; Back to catalog</a>

    <h1>${species.name().value()}</h1>

    @template.components.taxonomy(taxonomy = species.taxonomy())

    <div class="guilds">
        @for(var guild : species.guilds())
            <span class="badge guild-${guild.name().toLowerCase()}">${guild.name()}</span>
        @endfor
        @if(species.beneficial())
            <span class="badge beneficial">beneficial</span>
        @endif
    </div>

    @template.components.description(description = species.description())

    @if(species.sightingNotes() != null)
        <section>
            <h2>Sighting Notes</h2>
            <p>${species.sightingNotes()}</p>
        </section>
    @endif

    @if(!images.isEmpty())
        <section>
            <h2>Photo Gallery</h2>
            <div class="image-gallery">
                @for(var img : images)
                    <figure>
                        <img src="/insects/images/${img.resourceName().value()}"
                             alt="${species.name().value()}"
                             loading="lazy">
                    </figure>
                @endfor
            </div>
        </section>
    @endif

    @if(species.identificationFeatures() != null)
        <section>
            <h2>Identification Features</h2>
            <ul>
                @for(var feature : species.identificationFeatures().features())
                    <li>${feature}</li>
                @endfor
            </ul>
        </section>
    @endif

    @if(species.lifeStages() != null)
        <section>
            <h2>Life Stages</h2>
            @if(species.lifeStages().egg() != null)
                <h3>Egg</h3>
                <p>${species.lifeStages().egg().description()}</p>
            @endif
            @if(species.lifeStages().larva() != null)
                <h3>Larva
                    @if(species.lifeStages().larva().commonName() != null)
                        — ${species.lifeStages().larva().commonName()}
                    @endif
                </h3>
                <p>${species.lifeStages().larva().description()}</p>
                @if(species.lifeStages().larva().preyConsumption() != null)
                    <p><strong>Prey consumption:</strong> ${species.lifeStages().larva().preyConsumption()}</p>
                @endif
            @endif
            <h3>Adult</h3>
            <p><strong>Feeding:</strong> ${species.lifeStages().adult().feeding()}</p>
            <p><strong>Role:</strong> ${species.lifeStages().adult().role()}</p>
        </section>
    @endif

    @if(species.beneficialProfile() != null)
        <section>
            <h2>Beneficial Profile</h2>
            <p>${species.beneficialProfile().significance()}</p>
            @if(species.beneficialProfile().pestManagementValue() != null)
                <p><strong>Pest management:</strong> ${species.beneficialProfile().pestManagementValue()}</p>
            @endif
        </section>
    @endif

    @if(species.ecologicalSignificance() != null)
        <section>
            <h2>Ecological Significance</h2>
            @if(species.ecologicalSignificance().foodWebPosition() != null)
                <p><strong>Food web:</strong> ${species.ecologicalSignificance().foodWebPosition()}</p>
            @endif
            @if(species.ecologicalSignificance().indicatorValue() != null)
                <p><strong>Indicator value:</strong> ${species.ecologicalSignificance().indicatorValue()}</p>
            @endif
        </section>
    @endif

`)
