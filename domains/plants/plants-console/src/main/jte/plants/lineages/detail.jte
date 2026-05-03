@import com.naturalist.plants.heritage.SeedLineage

@param SeedLineage lineage

@template.layout.page(title = lineage.name().value(), content = @`
    <a href="/plants/cultivars/${lineage.cultivarName().value()}" class="naturalist-backlink">&larr; Back to
        cultivar</a>

    <h1>${lineage.name().value()}</h1>
    <p>
        <code>${lineage.name().value()}</code> &middot;
        lineage of <a href="/plants/cultivars/${lineage.cultivarName().value()}">${lineage.cultivarName().value()}</a>
    </p>

    <section>
        <h2>Provenance</h2>
        <ul>
            <li><strong>Originator:</strong> ${lineage.provenance().originator()}</li>
            <li><strong>Origin location:</strong> ${lineage.provenance().originLocation()}</li>
            <li><strong>Estimated generations:</strong> ${lineage.provenance().estimatedGenerations()}</li>
            @if(lineage.provenance().sourceNotes() != null)
                <li><strong>Source notes:</strong> ${lineage.provenance().sourceNotes()}</li>
            @endif
        </ul>
    </section>

    <section>
        <h2>Adaptation</h2>
        <ul>
            @if(lineage.hasActiveAdaptationProgram())
                <li><strong>Active adaptation program since</strong> ${lineage.adaptationStartYear()}</li>
            @else
                <li>No active adaptation program</li>
            @endif
            @if(lineage.selectionCriteria() != null)
                <li><strong>Selection criteria:</strong> ${lineage.selectionCriteria()}</li>
            @endif
            @if(lineage.notes() != null)
                <li><strong>Notes:</strong> ${lineage.notes()}</li>
            @endif
        </ul>
    </section>

    <section>
        <h2>Description</h2>
        <details open>
            <summary><strong>Preschool</strong></summary>
            <p>${lineage.description().preschool()}</p>
        </details>
        <details>
            <summary><strong>Elementary</strong></summary>
            <p>${lineage.description().elementary()}</p>
        </details>
        <details>
            <summary><strong>Secondary</strong></summary>
            <p>${lineage.description().secondary()}</p>
        </details>
        <details>
            <summary><strong>University</strong></summary>
            <p>${lineage.description().university()}</p>
        </details>
    </section>
`)
