@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectFunctionalRole
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.InsectImage
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param InsectSpecies species
@param InsectGenus genus
@param InsectFamily family
@param InsectOrder order
@param List<InsectImage> images
@param InsectFunctionalRole role = null
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param CsrfToken _csrf = null

@template.layout.page(title = species.name().value(), content = @`
    <a href="/insects">&larr; Back to catalog</a>

    <h1>${species.name().value()}</h1>

    <p>
        <a href="/insects/${species.name().value()}/life-stages" role="button" class="secondary">
            View Life Stages &rarr;
        </a>
    </p>

    <p class="taxonomy">
        ${order.order().value()} &middot; ${family.family().value()}
        &middot; <em>${genus.genus().value()} ${species.epithet().value()}</em>
    </p>

    @if(role != null)
        <div class="guilds">
            @for(var guild : role.guilds())
                <a href="/insects/guild/${guild.name()}"
                   class="badge guild-${guild.name().toLowerCase()}">${guild.name()}</a>
            @endfor
            @if(role.beneficial())
                <span class="badge beneficial">beneficial</span>
            @endif
        </div>
    @endif

    @template.components.description(
    preschool = descriptionPreschool,
    elementary = descriptionElementary,
    secondary = descriptionSecondary,
    university = descriptionUniversity)

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

    <section class="add-image">
        <h2>Add Photo</h2>
        <p>
            Drop the image file into
            <code>insects-repository-test/src/main/resources/insects/images/</code>
            first, then register its filename here.
        </p>
        <form method="post" action="/insects/${species.name().value()}/images">
            @if(_csrf != null)
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
            @endif
            <label for="resourceName">Filename</label>
            <input type="text" id="resourceName" name="resourceName"
                   placeholder="IMG_9047.HEIC" required>
            <button type="submit">Register photo</button>
        </form>
    </section>

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

    @if(species.egg() != null || species.larva() != null || species.pupa() != null || species.adult() != null)
        <section>
            <h2>Life Stages</h2>
            @if(species.egg() != null)
                <h3>Egg</h3>
                <p>${species.egg().description().secondary()}</p>
                @if(species.egg().layingPattern() != null)
                    <p><strong>Laying pattern:</strong> ${species.egg().layingPattern()}</p>
                @endif
                @if(species.egg().colorProgression() != null)
                    <p><strong>Colour progression:</strong> ${species.egg().colorProgression()}</p>
                @endif
                @if(species.egg().adaptiveSignificance() != null)
                    <p><strong>Adaptive significance:</strong> ${species.egg().adaptiveSignificance()}</p>
                @endif
            @endif
            @if(species.larva() != null)
                <h3>Larva</h3>
                <p>${species.larva().description().secondary()}</p>
                @if(species.larva().feedingStrategy() != null)
                    <p><strong>Feeding strategy:</strong> ${species.larva().feedingStrategy().name()}</p>
                @endif
                @if(!species.larva().hostPlants().isEmpty())
                    <p><strong>Host plants:</strong>
                        @for(var host : species.larva().hostPlants())
                            <span>${host.value()}</span>
                        @endfor
                    </p>
                @endif
                @if(!species.larva().parasitoidHosts().isEmpty())
                    <p><strong>Parasitoid hosts:</strong>
                        @for(var host : species.larva().parasitoidHosts())
                            <span>${host.value()}</span>
                        @endfor
                    </p>
                @endif
                @if(species.larva().instarProgression() != null)
                    <p><strong>Instar progression:</strong> ${species.larva().instarProgression()}</p>
                @endif
                @if(species.larva().remarkableBehavior() != null)
                    <p><strong>Remarkable behaviour:</strong> ${species.larva().remarkableBehavior()}</p>
                @endif
            @endif
            @if(species.pupa() != null)
                <h3>Pupa</h3>
                <p>${species.pupa().description().secondary()}</p>
                @if(species.pupa().appearance() != null)
                    <p><strong>Appearance:</strong> ${species.pupa().appearance()}</p>
                @endif
                @if(species.pupa().diapauseRegulation() != null)
                    <p><strong>Diapause
                            regulation:</strong> ${species.pupa().diapauseRegulation().getClass().getSimpleName()}</p>
                @endif
                @if(species.pupa().adaptiveSignificance() != null)
                    <p><strong>Adaptive significance:</strong> ${species.pupa().adaptiveSignificance()}</p>
                @endif
            @endif
            @if(species.adult() != null)
                <h3>Adult</h3>
                <p>${species.adult().description().secondary()}</p>
                @if(species.adult().feedingHabit() != null)
                    <p><strong>Feeding habit:</strong> ${species.adult().feedingHabit().name()}</p>
                @endif
                @if(species.adult().ecologicalRole() != null)
                    <p><strong>Role:</strong> ${species.adult().ecologicalRole()}</p>
                @endif
                @if(species.adult().lifespan() != null)
                    <p><strong>Lifespan:</strong> ${species.adult().lifespan()}</p>
                @endif
                @if(!species.adult().nectarSources().isEmpty())
                    <p><strong>Nectar sources:</strong>
                        @for(var source : species.adult().nectarSources())
                            <span>${source.value()}</span>
                        @endfor
                    </p>
                @endif
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
