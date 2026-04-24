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
            <a href="/insects/guild/${guild.name()}"
               class="badge guild-${guild.name().toLowerCase()}">${guild.name()}</a>
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
                @if(species.lifeStages().egg().layingPattern() != null)
                    <p><strong>Laying pattern:</strong> ${species.lifeStages().egg().layingPattern()}</p>
                @endif
                @if(species.lifeStages().egg().colorProgression() != null)
                    <p><strong>Colour progression:</strong> ${species.lifeStages().egg().colorProgression()}</p>
                @endif
                @if(species.lifeStages().egg().adaptiveSignificance() != null)
                    <p><strong>Adaptive significance:</strong> ${species.lifeStages().egg().adaptiveSignificance()}</p>
                @endif
            @endif
            @if(species.lifeStages().larva() != null)
                <h3>Larva
                    @if(species.lifeStages().larva().commonName() != null)
                        — ${species.lifeStages().larva().commonName()}
                    @endif
                </h3>
                <p>${species.lifeStages().larva().description()}</p>
                @if(!species.lifeStages().larva().hostPlants().isEmpty())
                    <p><strong>Host plants:</strong> ${String.join(", ", species.lifeStages().larva().hostPlants())}</p>
                @endif
                @if(!species.lifeStages().larva().preyTargets().isEmpty())
                    <p><strong>Prey:</strong> ${String.join(", ", species.lifeStages().larva().preyTargets())}</p>
                @endif
                @if(species.lifeStages().larva().preyConsumption() != null)
                    <p><strong>Prey consumption:</strong> ${species.lifeStages().larva().preyConsumption()}</p>
                @endif
                @if(species.lifeStages().larva().remarkableBehavior() != null)
                    <p><strong>Remarkable behaviour:</strong> ${species.lifeStages().larva().remarkableBehavior()}</p>
                @endif
            @endif
            @if(species.lifeStages().pupa() != null)
                <h3>Pupa</h3>
                <p>${species.lifeStages().pupa().description()}</p>
                @if(species.lifeStages().pupa().appearance() != null)
                    <p><strong>Appearance:</strong> ${species.lifeStages().pupa().appearance()}</p>
                @endif
                @if(species.lifeStages().pupa().diapauseRegulation() != null)
                    <p><strong>Diapause regulation:</strong> ${species.lifeStages().pupa().diapauseRegulation()}</p>
                @endif
                @if(species.lifeStages().pupa().adaptiveSignificance() != null)
                    <p><strong>Adaptive significance:</strong> ${species.lifeStages().pupa().adaptiveSignificance()}</p>
                @endif
            @endif
            <h3>Adult</h3>
            <p><strong>Feeding:</strong> ${species.lifeStages().adult().feeding()}</p>
            <p><strong>Role:</strong> ${species.lifeStages().adult().role()}</p>
            @if(species.lifeStages().adult().flightPeriod() != null)
                <p><strong>Flight period:</strong> ${species.lifeStages().adult().flightPeriod()}</p>
            @endif
            @if(species.lifeStages().adult().lifespan() != null)
                <p><strong>Lifespan:</strong> ${species.lifeStages().adult().lifespan()}</p>
            @endif
            @if(species.lifeStages().adult().attraction() != null)
                <p><strong>Attraction:</strong> ${species.lifeStages().adult().attraction()}</p>
            @endif
            @if(!species.lifeStages().adult().supportedBy().isEmpty())
                <p><strong>Supported by:</strong> ${String.join(", ", species.lifeStages().adult().supportedBy())}</p>
            @endif
        </section>
    @endif

    @if(species.chemicalDefense() != null)
        <section>
            <h2>Chemical Defence</h2>
            <p><strong>Mechanism:</strong> ${species.chemicalDefense().mechanism()}</p>
            @if(species.chemicalDefense().sourceCompounds() != null)
                <p><strong>Source compounds:</strong> ${species.chemicalDefense().sourceCompounds()}</p>
            @endif
            @if(species.chemicalDefense().aposematicSignal() != null)
                <p><strong>Aposematic signal:</strong> ${species.chemicalDefense().aposematicSignal()}</p>
            @endif
            @if(!species.chemicalDefense().protectedStages().isEmpty())
                <p><strong>Protected stages:</strong>
                    @for(var stage : species.chemicalDefense().protectedStages())
                        <span class="badge stage">${stage.name()}</span>
                    @endfor
                </p>
            @endif
        </section>
    @endif

    @if(species.voltinism() != null)
        <section>
            <h2>Voltinism</h2>
            <p><strong>Pattern:</strong> ${species.voltinism().pattern().name()}</p>
            @if(species.voltinism().notes() != null)
                <p>${species.voltinism().notes()}</p>
            @endif
        </section>
    @endif

    @if(species.habitatRequirements() != null)
        <section>
            <h2>Habitat Requirements</h2>
            @if(species.habitatRequirements().nectarSources() != null)
                <p><strong>Nectar sources:</strong> ${species.habitatRequirements().nectarSources()}</p>
            @endif
            @if(species.habitatRequirements().shelter() != null)
                <p><strong>Shelter:</strong> ${species.habitatRequirements().shelter()}</p>
            @endif
            @if(species.habitatRequirements().preyAvailability() != null)
                <p><strong>Prey availability:</strong> ${species.habitatRequirements().preyAvailability()}</p>
            @endif
            @if(species.habitatRequirements().lighting() != null)
                <p><strong>Lighting response:</strong> ${species.habitatRequirements().lighting()}</p>
            @endif
            @if(species.habitatRequirements().elevationRange() != null)
                <p><strong>Elevation:</strong> ${species.habitatRequirements().elevationRange()}</p>
            @endif
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
