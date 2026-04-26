@import com.naturalist.chemistry.compound.Compound
@import com.naturalist.chemistry.product.Product
@import java.util.List

@param Product product
@param List<Compound> compounds = java.util.List.of()

@template.layout.page(title = product.displayName(), content = @`
    <a href="/chemistry/products" class="naturalist-backlink">&larr; Back to products</a>

    <h1>${product.displayName()}</h1>
    <p><code>${product.name().value()}</code></p>

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Slug:</strong> <code>${product.name().value()}</code></li>
            <li><strong>Display name:</strong> ${product.displayName()}</li>
        </ul>
    </section>

    <section>
        <h2>Compounds (${product.compounds().size()})</h2>
        @if(!compounds.isEmpty())
            <ul>
                @for(var c : compounds)
                    <li>
                        <a href="/chemistry/${c.name().value()}">${c.commonName()}</a>
                        &middot; <code>${c.name().value()}</code>
                        &middot; <em>${c.formula()}</em>
                    </li>
                @endfor
            </ul>
        @else
            <ul>
                @for(var compoundName : product.compounds())
                    <li><code>${compoundName.value()}</code></li>
                @endfor
            </ul>
        @endif
    </section>

    @if(!product.properties().isEmpty())
        <section>
            <h2>Properties</h2>
            <ul>
                @for(var entry : product.properties().entrySet())
                    <li><strong>${entry.getKey()}:</strong> ${entry.getValue()}</li>
                @endfor
            </ul>
        </section>
    @endif
`)
