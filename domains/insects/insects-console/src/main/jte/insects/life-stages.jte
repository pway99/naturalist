@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import com.naturalist.insects.lifestage.LifeStage
@import java.util.List

@param InsectSpecies species
@param InsectFamily family
@param InsectOrder order
@param List<LifeStage> stages
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param CladeTrail cladeTrail = null
@param List<AncestorIntro> ancestorIntros = List.of()

!{var commonName = species.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(species.name().value());}

@template.layout.page(title = commonName + " — Life Stages", content = @`
    @template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    <section class="life-stages">
        <header class="plate-heading">
            <p class="plate-eyebrow">Plate &mdash; Life History</p>
            <h1>${commonName}</h1>
            <p class="plate-subtitle">
                ${order.order().value()} &middot; ${family.family().value()}
            </p>
        </header>

        @if(stages.isEmpty())
            <p class="plate-note"><em>No life stage plates catalogued for this species yet.</em></p>
        @else
            @for(var stage : stages)
                <article class="stage-plate stage-${stage.kind().name().toLowerCase()}">
                    <header class="stage-header">
                        <span class="stage-ordinal">${String.valueOf(stage.kind().ordinal() + 1)}</span>
                        <div>
                            <h2>${stage.kind().name().charAt(0) + stage.kind().name().substring(1).toLowerCase()}</h2>
                            <p class="stage-slug">${stage.name().value()}</p>
                        </div>
                    </header>

                    <div class="stage-description">
                        <p class="lead">${stage.description().secondary()}</p>
                        <details>
                            <summary>Further reading</summary>
                            <p><strong>A child might say&hellip;</strong> ${stage.description().preschool()}</p>
                            <p><strong>For the young naturalist&hellip;</strong> ${stage.description().elementary()}</p>
                            <p><strong>For the scholar&hellip;</strong> ${stage.description().university()}</p>
                        </details>
                    </div>

                    @template.insects.stageFacts(stage = stage)
                </article>
            @endfor
        @endif
    </section>
`)
