@import com.naturalist.insects.InsectOrder
@import com.naturalist.data.Page

@param Page<InsectOrder> orderPage

@template.layout.page(title = "Insect Orders", content = @`
    @template.insects.nav(active = "order")
    <h1>Insect Orders</h1>
    <p>Order-rank records in the Oak Vista catalog.</p>

    <div class="entity-grid">
        @for(var o : orderPage.content())
            !{var commonName = o.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(o.name().value());}
            <article>
                <header>
                    <a href="/insects/orders/${o.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>order</dt>
                    <dd><em>${o.order().value()}</em></dd>
                </dl>
            </article>
        @endfor
    </div>

    @template.components.pager(page = orderPage, baseUrl = "/insects/orders")
`)
