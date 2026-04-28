@import com.naturalist.plants.phytochemistry.PhytochemicalConstituent

@param PhytochemicalConstituent constituent

@template.layout.page(title = constituent.name().value(), content = @`
    <a href="/plants/phytochemistry" class="naturalist-backlink">&larr; Back to phytochemistry</a>

    <h1>${constituent.name().value()}</h1>
    <p>
        <a href="/plants/${constituent.plantName().value()}"><code>${constituent.plantName().value()}</code></a>
        &middot;
        <a href="/chemistry/${constituent.compoundName().value()}"><code>${constituent.compoundName().value()}</code></a>
    </p>

    <section>
        <h2>Classification</h2>
        <ul>
            <li><strong>Category:</strong> <span class="badge">${constituent.category().name()}</span></li>
            <li><strong>Induction:</strong> ${constituent.induction().name()}</li>
            <li>
                <strong>Tissues:</strong>
                @for(var tissue : constituent.tissues())
                    <span class="badge">${tissue.name()}</span>
                @endfor
            </li>
        </ul>
    </section>

    <section>
        <h2>Roles in this plant</h2>
        <p class="description-preview">
            @for(var role : constituent.roles())
                <span class="badge">${role.getClass().getSimpleName()}</span>
            @endfor
        </p>
        <p class="description-preview">
            <strong>Axis rollups:</strong>
            @if(constituent.isDefensive())
                <span class="badge">Defense</span>
            @endif
            @if(constituent.isSignaling())
                <span class="badge">Signaling</span>
            @endif
            @if(constituent.mediatesEnvironmentalStress())
                <span class="badge">Environmental</span>
            @endif
            @if(constituent.hasMedicinalApplication())
                <span class="badge">Medicinal</span>
            @endif
            @if(constituent.hasCommercialApplication())
                <span class="badge">Commercial</span>
            @endif
            @if(constituent.isToxicToMammals())
                <span class="badge guild-apex_predator">Toxic to mammals</span>
            @endif
        </p>
    </section>

    <section>
        <h2>Description</h2>
        <details open>
            <summary><strong>Preschool</strong></summary>
            <p>${constituent.description().preschool()}</p>
        </details>
        <details>
            <summary><strong>Elementary</strong></summary>
            <p>${constituent.description().elementary()}</p>
        </details>
        <details>
            <summary><strong>Secondary</strong></summary>
            <p>${constituent.description().secondary()}</p>
        </details>
        <details>
            <summary><strong>University</strong></summary>
            <p>${constituent.description().university()}</p>
        </details>
    </section>

    @if(constituent.notes() != null)
        <section>
            <h2>Notes</h2>
            <p>${constituent.notes()}</p>
        </section>
    @endif
`)
