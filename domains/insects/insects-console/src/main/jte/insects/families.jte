@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectOrderName
@import com.naturalist.data.Page
@import java.util.Map

@param Page<InsectFamily> familyPage
@param Map<InsectOrderName, InsectOrder> orderByName
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param String cladePreschool = ""
@param String cladeElementary = ""
@param String cladeSecondary = ""
@param String cladeUniversity = ""

@template.layout.page(title = "Insect Families", content = @`
    @template.insects.cladeIntro(
        cladePreschool = cladePreschool,
        cladeElementary = cladeElementary,
        cladeSecondary = cladeSecondary,
        cladeUniversity = cladeUniversity)
    <nav class="insects-subnav" aria-label="Insects catalog sub-navigation">
        <a href="/insects/orders" aria-current="false">Order</a>
        <a href="/insects/families" aria-current="page">Family</a>
        <a href="/insects/genera" aria-current="false">Genus</a>
        <a href="/insects/species" aria-current="false">Species</a>
    </nav>
    <h1>Insect Families</h1>
    <p>Family-rank records in the Oak Vista catalog.</p>

    <div class="entity-grid">
        @for(var f : familyPage.content())
            !{var order = orderByName.get(f.orderName());}
            !{var commonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.name().value());}
            <article>
                <header>
                    <a href="/insects/families/${f.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>order</dt>
                    <dd>${order.order().value()}</dd>
                    <dt>family</dt>
                    <dd><em>${f.family().value()}</em></dd>
                </dl>
                !{var images = gallery.forEntity(f.name()).stream().toList();}
                @if(!images.isEmpty())
                    @template.insects.cardImages(images = images, linkUrl = "/insects/families/" + f.name().value(), alt = commonName)
                @endif
            </article>
        @endfor
    </div>

    @template.components.pager(page = familyPage, baseUrl = "/insects/families")

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
