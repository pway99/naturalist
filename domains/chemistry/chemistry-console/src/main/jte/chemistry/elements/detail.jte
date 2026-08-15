@import com.naturalist.chemistry.element.Element

@param Element element

@template.layout.page(title = element.name().value(), content = @`
    <a href="/chemistry/elements" class="naturalist-backlink">&larr; Back to elements</a>

    <h1>${element.symbol()} &mdash; ${element.name().value()}</h1>
    <p><code>${element.name().value()}</code></p>

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Symbol:</strong> <code>${element.symbol()}</code></li>
            <li><strong>Atomic weight:</strong> ${element.atomicWeight().value().toString()} g/mol</li>
            <li><strong>Ionic form:</strong> <code>${element.ionicForm()}</code></li>
            @if(element.isCation())
                <li><strong>Charge:</strong> +${element.ionicCharge()} &mdash; a <span>cation</span>,
                    held on soil surfaces and exchanged from them</li>
            @elseif(element.isAnion())
                <li><strong>Charge:</strong> ${element.ionicCharge()} &mdash; an <span>anion</span>,
                    which moves with soil water rather than being held</li>
            @else
                <li><strong>Charge:</strong> <span class="not-reported">uncharged</span> in the form
                    plants take up &mdash; neither held on exchange sites nor repelled from them</li>
            @endif
        </ul>
    </section>
`)
