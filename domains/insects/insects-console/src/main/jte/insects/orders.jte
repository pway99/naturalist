@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.data.Page
@import java.util.List

@param Page<InsectOrder> orderPage
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param List<AncestorIntro> ancestorIntros = List.of()

@template.layout.page(title = "Insect Orders", content = @`
    @template.insects.nav(active = "order", breadcrumb = breadcrumb)
    @template.insects.ancestorIntros(intros = ancestorIntros)

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
                    <dd>${o.order().value()}</dd>
                </dl>
                !{var images = gallery.forEntity(o.name()).stream().toList();}
                @if(!images.isEmpty())
                    @template.insects.cardImages(images = images, linkUrl = "/insects/orders/" + o.name().value(), alt = commonName)
                @endif
            </article>
        @endfor
    </div>

    @template.components.pager(page = orderPage, baseUrl = "/insects/orders")
`)
