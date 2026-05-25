@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectSpecies
@import java.util.List

@param InsectGenus genus
@param InsectFamily family
@param InsectOrder order
@param List<InsectSpecies> species
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = genus.genus().value(), content = @`
    @template.insects.nav(active = "genus")
    <h1><em>${genus.genus().value()}</em></h1>
    <p class="taxonomy">
        <a href="/insects/orders/${order.name().value()}">${order.order().value()}</a> &middot;
        <a href="/insects/families/${genus.familyName().value()}">${family.family().value()}</a>
    </p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    <section>
        <h2>Species in this genus</h2>
        @if(species.isEmpty())
            <p><em>No species catalogued under this genus yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var s : species)
                    !{var commonName = s.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(s.name().value());}
                    <article>
                        <header>
                            <a href="/insects/${s.name().value()}">
                                <strong>${commonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>species</dt>
                            <dd><em>${s.epithet().value()}</em></dd>
                        </dl>
                        !{var images = gallery.forEntity(s.name()).stream().toList();}
                        @if(!images.isEmpty())
                            @template.insects.cardImages(images = images, linkUrl = "/insects/" + s.name().value(), alt = commonName)
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
