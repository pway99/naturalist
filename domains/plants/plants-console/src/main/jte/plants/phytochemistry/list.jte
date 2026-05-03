@import com.naturalist.plants.phytochemistry.PhytochemicalConstituent
@import java.util.List

@param List<PhytochemicalConstituent> constituents

@template.layout.page(title = "Phytochemistry Catalog", content = @`
    <a href="/plants" class="naturalist-backlink">&larr; Back to plants</a>

    <h1>Phytochemistry Catalog</h1>
    <p>${constituents.size()} plant-compound links documented. Each entry records
        one compound in one plant and the role(s) it plays there.</p>

    <div class="entity-grid">
        @for(var c : constituents)
            <article>
                <header>
                    <a href="/plants/phytochemistry/${c.name().value()}">
                        <strong>${c.name().value()}</strong>
                    </a>
                    <span class="badge">${c.category().name()}</span>
                    <span class="badge">${c.induction().name()}</span>
                    @if(c.isToxicToMammals())
                        <span class="badge guild-apex_predator">toxic</span>
                    @endif
                </header>
                <p class="taxonomy">
                    plant: <a href="/plants/${c.plantName().value()}"><code>${c.plantName().value()}</code></a><br>
                    compound: <a href="/chemistry/${c.compoundName().value()}"><code>${c.compoundName().value()}</code></a>
                </p>
                <p class="description-preview">
                    @if(c.isDefensive())
                        <span class="badge">Defense</span>
                    @endif
                    @if(c.isSignaling())
                        <span class="badge">Signaling</span>
                    @endif
                    @if(c.mediatesEnvironmentalStress())
                        <span class="badge">Environmental</span>
                    @endif
                    @if(c.hasMedicinalApplication())
                        <span class="badge">Medicinal</span>
                    @endif
                    @if(c.hasCommercialApplication())
                        <span class="badge">Commercial</span>
                    @endif
                </p>
                <p class="description-preview">${c.description().preschool()}</p>
                <footer>
                    <a href="/plants/phytochemistry/${c.name().value()}">
                        Constituent details &rarr;
                    </a>
                </footer>
            </article>
        @endfor
    </div>
`)
