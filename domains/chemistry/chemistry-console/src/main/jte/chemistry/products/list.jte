@import com.naturalist.chemistry.product.Product
@import java.util.List

@param List<Product> products

@template.layout.page(title = "Products Catalog", content = @`
    <a href="/chemistry" class="naturalist-backlink">&larr; Back to compounds</a>

    <h1>Products Catalog</h1>
    <p>${products.size()} products catalogued.</p>

    <div class="entity-grid">
        @for(var p : products)
            <article>
                <header>
                    <a href="/chemistry/products/${p.name().value()}">
                        <strong>${p.displayName()}</strong>
                    </a>
                </header>
                <p class="taxonomy">
                    <code>${p.name().value()}</code>
                </p>
                <p class="description-preview">
                    <strong>Compounds:</strong>
                    @for(var compoundName : p.compounds())
                        <a href="/chemistry/${compoundName.value()}"><code>${compoundName.value()}</code></a>
                    @endfor
                    @if(!p.properties().isEmpty())
                        <br><strong>Properties:</strong> ${p.properties().size()}
                    @endif
                </p>
                <footer>
                    <a href="/chemistry/products/${p.name().value()}">
                        Product details &rarr;
                    </a>
                </footer>
            </article>
        @endfor
    </div>
`)
