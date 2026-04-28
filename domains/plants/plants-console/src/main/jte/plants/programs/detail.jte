@import com.naturalist.plants.management.PlantProgram

@param PlantProgram program

@template.layout.page(title = program.name().value(), content = @`
    <a href="/plants/${program.plantName().value()}" class="naturalist-backlink">&larr; Back to plant</a>

    <h1>${program.name().value()}</h1>
    <p>
        <code>${program.name().value()}</code> &middot;
        program for <a href="/plants/${program.plantName().value()}">${program.plantName().value()}</a>
        @if(program.hasConstraint())
            <span class="badge guild-apex_predator">constraint-bearing</span>
        @endif
    </p>

    @if(program.hasConstraint())
        <section>
            <h2>Constraint</h2>
            <p>${program.constraint()}</p>
        </section>
    @endif

    @if(program.notes() != null)
        <section>
            <h2>Notes</h2>
            <p>${program.notes()}</p>
        </section>
    @endif

    <section>
        <h2>Description</h2>
        <details open>
            <summary><strong>Preschool</strong></summary>
            <p>${program.description().preschool()}</p>
        </details>
        <details>
            <summary><strong>Elementary</strong></summary>
            <p>${program.description().elementary()}</p>
        </details>
        <details>
            <summary><strong>Secondary</strong></summary>
            <p>${program.description().secondary()}</p>
        </details>
        <details>
            <summary><strong>University</strong></summary>
            <p>${program.description().university()}</p>
        </details>
    </section>
`)
