@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectOrder
@import com.naturalist.data.Page

@param Page<InsectOrder> orderPage
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param String cladePreschool = ""
@param String cladeElementary = ""
@param String cladeSecondary = ""
@param String cladeUniversity = ""

@template.layout.page(title = "Insect Orders", content = @`
    @template.insects.cladeIntro(
        cladePreschool = cladePreschool,
        cladeElementary = cladeElementary,
        cladeSecondary = cladeSecondary,
        cladeUniversity = cladeUniversity)
    <nav class="insects-subnav" aria-label="Insects catalog sub-navigation">
        <a href="/insects/orders" aria-current="page">Order</a>
        <a href="/insects/families" aria-current="false">Family</a>
        <a href="/insects/genera" aria-current="false">Genus</a>
        <a href="/insects/species" aria-current="false">Species</a>
    </nav>
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
                !{var images = gallery.forEntity(o.name()).stream().toList();}
                @if(!images.isEmpty())
                    @template.insects.cardImages(images = images, linkUrl = "/insects/orders/" + o.name().value(), alt = commonName)
                @endif
            </article>
        @endfor
    </div>

    @template.components.pager(page = orderPage, baseUrl = "/insects/orders")

    <script>
        (function () {
            if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                return;
            }
            var interval = 3500;
            document.querySelectorAll('.card-image[data-rotate]').forEach(function (el) {
                var imgs = el.querySelectorAll('img');
                if (imgs.length < 2) return;
                var i = 0;
                setInterval(function () {
                    imgs[i].classList.remove('card-image-active');
                    i = (i + 1) % imgs.length;
                    imgs[i].classList.add('card-image-active');
                }, interval);
            });
        }());
    </script>
`)
