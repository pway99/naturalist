@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param InsectFamily family
@param InsectOrder order
@param List<InsectGenus> genera
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = List.of()

@template.layout.page(title = family.family().value(), content = @`
    @template.insects.nav(active = "family", breadcrumb = breadcrumb)
    <h1>${family.family().value()}</h1>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    <section>
        <h2>Genera in this family</h2>
        @if(genera.isEmpty())
            <p><em>No genera catalogued under this family yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var g : genera)
                    !{var commonName = g.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(g.name().value());}
                    <article>
                        <header>
                            <a href="/insects/genera/${g.name().value()}">
                                <strong>${commonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>genus</dt>
                            <dd><em>${g.genus().value()}</em></dd>
                        </dl>
                        !{var images = gallery.forEntity(g.name()).stream().toList();}
                        @if(!images.isEmpty())
                            @template.insects.cardImages(images = images, linkUrl = "/insects/genera/" + g.name().value(), alt = commonName)
                        @endif
                    </article>
                @endfor
            </div>
        @endif
    </section>

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
