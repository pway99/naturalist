@import com.naturalist.insects.InsectImage
@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.InsectSpeciesName
@import com.naturalist.insects.FunctionalGuild
@import com.naturalist.data.Page
@import java.util.List
@import java.util.Map

@param Page<InsectSpecies> speciesPage
@param Map<InsectSpeciesName, List<InsectImage>> imagesBySpecies = Map.of()

@template.layout.page(title = "Insect Catalog", content = @`
    <h1>Insect Catalog</h1>
    <p>Insect species documented at Oak Vista.</p>

    <div class="entity-grid">
        @for(var s : speciesPage.content())
            !{var images = imagesBySpecies.getOrDefault(s.name(), List.of());}
            !{var commonName = s.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(s.name().value());}
            <article>
                <header>
                    <a href="/insects/${s.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                    @if(s.beneficial())
                        <span class="badge beneficial">beneficial</span>
                    @endif
                </header>
                <dl class="taxonomy">
                    <dt>fam.</dt>
                    <dd>${s.taxonomy().family().value()}</dd>
                    @if(s.taxonomy().genus() != null)
                        <dt>gen.</dt>
                        <dd><em>${s.taxonomy().genus().value()}</em></dd>
                    @endif
                    @if(s.taxonomy().species() != null)
                        <dt>sp.</dt>
                        <dd><em>${s.taxonomy().species().value()}</em></dd>
                    @endif
                </dl>
                @if(!images.isEmpty())
                    <a href="/insects/${s.name().value()}" class="card-image-link">
                        <figure class="card-image" data-rotate="${images.size() > 1}">
                            @for(int i = 0; i < images.size(); i++)
                                <img src="/insects/images/${images.get(i).resourceName().value()}"
                                     alt="${commonName}"
                                     loading="lazy"
                                     class="${i == 0 ? "card-image-active" : ""}">
                            @endfor
                        </figure>
                    </a>
                @endif
                <div class="guilds">
                    @for(var guild : s.guilds())
                        <a href="/insects/guild/${guild.name()}"
                           class="badge guild-${guild.name().toLowerCase()}">${guild.name()}</a>
                    @endfor
                </div>
                <footer>
                    <a href="/insects/${s.name().value()}/life-stages" class="secondary">
                        Life Stages &rarr;
                    </a>
                </footer>
            </article>
        @endfor
    </div>

    @template.components.pager(page = speciesPage, baseUrl = "/insects")

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
