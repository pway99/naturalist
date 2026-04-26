@import com.naturalist.chemistry.compound.Compound
@import java.util.List

@param List<Compound> compounds

@template.layout.page(title = "Chemistry Catalog", content = @`
    <h1>Chemistry Catalog</h1>
    <p>${compounds.size()} compounds catalogued.</p>

    <div class="entity-grid">
        @for(var c : compounds)
            <article>
                <header>
                    <a href="/chemistry/${c.name().value()}">
                        <strong>${c.commonName()}</strong>
                    </a>
                    @if(!c.isSafe())
                        <span class="badge guild-apex_predator">hazardous</span>
                    @endif
                    @if(c.isFumigant())
                        <span class="badge guild-pollinator">fumigant</span>
                    @endif
                </header>
                <p class="taxonomy">
                    <code>${c.name().value()}</code>
                </p>
                <p class="description-preview">
                    <strong>Formula:</strong> <code>${c.formula()}</code><br>
                    <strong>Nature:</strong> ${c.compoundInfo().chemicalNature().name()}
                    &middot; <strong>Form:</strong> ${c.compoundInfo().physicalForm().name()}<br>
                    <strong>pH:</strong> ${c.phCharacter().name()}
                </p>
                <footer>
                    <a href="/chemistry/${c.name().value()}">
                        Compound details &rarr;
                    </a>
                </footer>
            </article>
        @endfor
    </div>
`)
