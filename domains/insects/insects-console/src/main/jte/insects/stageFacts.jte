@import com.naturalist.insects.lifestage.AdultStage
@import com.naturalist.insects.lifestage.EggStage
@import com.naturalist.insects.lifestage.LarvaStage
@import com.naturalist.insects.lifestage.LifeStage
@import com.naturalist.insects.lifestage.PupaStage

@param LifeStage stage

<dl class="stage-facts">
    <dt>Phenology</dt>
    <dd>
        @for(var window : stage.phenology().windows())
            <div>
                @if(window.cohortLabel() != null)
                    <em>${window.cohortLabel()}:</em>
                @endif
                onset ${window.onset().toString()}
                @if(window.peak() != null)
                    &middot; peak ${window.peak().toString()}
                @endif
                &middot; tail ${window.tail().toString()}
            </div>
        @endfor
        @if(stage.phenology().notes() != null)
            <p class="aside">${stage.phenology().notes()}</p>
        @endif
    </dd>

    <dt>Habitat</dt>
    <dd>
        @if(stage.habitat().substrate() != null)
            <div><em>Substrate:</em> ${stage.habitat().substrate()}</div>
        @endif
        @if(stage.habitat().microclimate() != null)
            <div><em>Microclimate:</em> ${stage.habitat().microclimate()}</div>
        @endif
        @if(stage.habitat().spatialNotes() != null)
            <div><em>Spatial notes:</em> ${stage.habitat().spatialNotes()}</div>
        @endif
    </dd>

    @if(stage.chemistryRole() != null)
        <dt>Chemistry role</dt>
        <dd>${stage.chemistryRole().toString()}</dd>
    @endif

    @if(stage instanceof EggStage egg)
        @if(egg.layingPattern() != null)
            <dt>Laying pattern</dt>
            <dd>${egg.layingPattern()}</dd>
        @endif
        @if(egg.colorProgression() != null)
            <dt>Colour progression</dt>
            <dd>${egg.colorProgression()}</dd>
        @endif
        @if(egg.adaptiveSignificance() != null)
            <dt>Adaptive significance</dt>
            <dd>${egg.adaptiveSignificance()}</dd>
        @endif
    @endif

    @if(stage instanceof LarvaStage larva)
        @if(larva.feedingStrategy() != null)
            <dt>Feeding strategy</dt>
            <dd>${larva.feedingStrategy().name()}</dd>
        @endif
        @if(!larva.hostPlants().isEmpty())
            <dt>Host plants</dt>
            <dd>
                @for(var host : larva.hostPlants())
                    <span class="token">${host.value()}</span>
                @endfor
            </dd>
        @endif
        @if(!larva.parasitoidHosts().isEmpty())
            <dt>Parasitoid hosts</dt>
            <dd>
                @for(var host : larva.parasitoidHosts())
                    <span class="token">${host.value()}</span>
                @endfor
            </dd>
        @endif
        @if(larva.instarProgression() != null)
            <dt>Instar progression</dt>
            <dd>${larva.instarProgression()}</dd>
        @endif
        @if(larva.remarkableBehavior() != null)
            <dt>Remarkable behaviour</dt>
            <dd>${larva.remarkableBehavior()}</dd>
        @endif
    @endif

    @if(stage instanceof PupaStage pupa)
        @if(pupa.appearance() != null)
            <dt>Appearance</dt>
            <dd>${pupa.appearance()}</dd>
        @endif
        @if(pupa.diapauseRegulation() != null)
            <dt>Diapause regulation</dt>
            <dd>${pupa.diapauseRegulation().getClass().getSimpleName()}</dd>
        @endif
        @if(pupa.adaptiveSignificance() != null)
            <dt>Adaptive significance</dt>
            <dd>${pupa.adaptiveSignificance()}</dd>
        @endif
    @endif

    @if(stage instanceof AdultStage adult)
        @if(adult.feedingHabit() != null)
            <dt>Feeding habit</dt>
            <dd>${adult.feedingHabit().name()}</dd>
        @endif
        @if(adult.ecologicalRole() != null)
            <dt>Ecological role</dt>
            <dd>${adult.ecologicalRole()}</dd>
        @endif
        @if(adult.lifespan() != null)
            <dt>Lifespan</dt>
            <dd>${adult.lifespan()}</dd>
        @endif
        @if(!adult.nectarSources().isEmpty())
            <dt>Nectar sources</dt>
            <dd>
                @for(var source : adult.nectarSources())
                    <span class="token">${source.value()}</span>
                @endfor
            </dd>
        @endif
    @endif
</dl>
