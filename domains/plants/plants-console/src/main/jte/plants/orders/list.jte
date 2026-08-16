@import com.naturalist.data.Page
@import com.naturalist.plants.PlantOrder

@param Page<PlantOrder> ordersPage

@template.layout.page(title = "Plant Orders", content = @`
    <a href="/plants" class="naturalist-backlink">&larr; Back to catalog</a>

    <h1>Plant Orders</h1>
    <p>
        The top rung of the plant chain. A flower recognised as Lamiales — square stem,
        bilabiate corolla — without a resolved family has a permanent home here.
    </p>

    <div class="entity-grid">
        @for(var o : ordersPage.content())
            !{var commonName = o.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(o.order().value());}
            <article>
                <header>
                    <a href="/plants/orders/${o.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <p class="taxonomy"><em>${o.order().value()}</em></p>
                <p class="description-preview">${o.description().preschool()}</p>
                <footer>
                    <a href="/plants/orders/${o.name().value()}">
                        Order details &rarr;
                    </a>
                </footer>
            </article>
        @endfor
    </div>

    @template.components.pager(page = ordersPage, baseUrl = "/plants/orders")
`)
