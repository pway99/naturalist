@import com.naturalist.data.Page
@import com.naturalist.plants.PlantOrder
@import com.naturalist.plants.BreadcrumbSegment
@import com.naturalist.clades.Clade
@import java.util.List

@param Page<PlantOrder> ordersPage
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<Clade> cladeTrail = java.util.List.of()

@template.layout.page(title = "Plant Orders", content = @`
    @template.plants.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    <h1>Plant Orders</h1>
    <p>
        Order-rank records in the Oak Vista catalog — the top of the plant chain. A flower
        recognised as Lamiales, without a resolved family, has a permanent home here.
        <a href="/plants/species">Browse all species &rarr;</a>
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
                <dl class="taxonomy">
                    <dt>order</dt>
                    <dd>${o.order().value()}</dd>
                </dl>
                <p class="description-preview">${o.description().preschool()}</p>
            </article>
        @endfor
    </div>

    @template.components.pager(page = ordersPage, baseUrl = "/plants/orders")
`)
