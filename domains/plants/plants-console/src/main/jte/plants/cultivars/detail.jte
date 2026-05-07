@import com.naturalist.plants.cultivar.Cultivar
@import com.naturalist.plants.heritage.SeedLineage
@import java.util.List

@param Cultivar cultivar
@param List<SeedLineage> lineages = java.util.List.of()

@template.layout.page(title = cultivar.commonName(), content = @`
    <a href="/plants/${cultivar.plantName().value()}" class="naturalist-backlink">&larr; Back to plant</a>

    <h1>${cultivar.commonName()}</h1>
    <p>
        <code>${cultivar.name().value()}</code> &middot;
        cultivar of <a href="/plants/${cultivar.plantName().value()}">${cultivar.plantName().value()}</a>
    </p>

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Variety type:</strong> ${cultivar.varietyType().name()}</li>
            @if(cultivar.fruitType() != null)
                <li><strong>Fruit type:</strong> ${cultivar.fruitType().name()}</li>
            @endif
            <li><strong>Seed saving:</strong> ${cultivar.seedSavingPolicy().name()}</li>
            @if(cultivar.seedSource() != null)
                <li><strong>Seed source:</strong> ${cultivar.seedSource()}</li>
            @endif
            @if(cultivar.gardenNotes() != null)
                <li><strong>Garden notes:</strong> ${cultivar.gardenNotes()}</li>
            @endif
        </ul>
    </section>

    <section>
        <h2>Behavioral predicates</h2>
        <p class="description-preview">
            @if(cultivar.breedsTrueFromSeed())
                <span class="badge">Breeds true from seed</span>
            @endif
            @if(cultivar.requiresSeedSaving())
                <span class="badge">Requires seed saving</span>
            @endif
            @if(cultivar.isSauceVariety())
                <span class="badge">Sauce variety</span>
            @endif
        </p>
    </section>

    <section>
        <h2>Description</h2>
        <details open>
            <summary><strong>Preschool</strong></summary>
            <p>${cultivar.description().preschool()}</p>
        </details>
        <details>
            <summary><strong>Elementary</strong></summary>
            <p>${cultivar.description().elementary()}</p>
        </details>
        <details>
            <summary><strong>Secondary</strong></summary>
            <p>${cultivar.description().secondary()}</p>
        </details>
        <details>
            <summary><strong>University</strong></summary>
            <p>${cultivar.description().university()}</p>
        </details>
    </section>

    @if(!lineages.isEmpty())
        <section>
            <h2>Seed lineages (${lineages.size()})</h2>
            <ul>
                @for(var l : lineages)
                    <li>
                        <a href="/plants/lineages/${l.name().value()}"><strong>${l.name().value()}</strong></a>
                        @if(l.hasActiveAdaptationProgram())
                            <span class="badge">Adaptation program since ${l.adaptationStartYear()}</span>
                        @endif
                    </li>
                @endfor
            </ul>
        </section>
    @endif
`)
