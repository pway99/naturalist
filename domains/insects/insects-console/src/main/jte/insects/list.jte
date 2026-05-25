@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectFamilyName
@import com.naturalist.insects.InsectFunctionalRole
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectGenusName
@import com.naturalist.insects.InsectImage
@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.InsectSpeciesName
@import com.naturalist.insects.FunctionalGuild
@import com.naturalist.data.Page
@import java.util.List
@import java.util.Map
@import java.util.Set

@param Page<InsectSpecies> speciesPage
@param Map<InsectSpeciesName, List<InsectImage>> imagesBySpecies = Map.of()
@param Map<InsectSpeciesName, InsectFunctionalRole> rolesBySpecies = Map.of()
@param Map<InsectFamilyName, InsectFamily> familyByName = Map.of()
@param Map<InsectGenusName, InsectGenus> genusByName = Map.of()
@param String cladePreschool = ""
@param String cladeElementary = ""
@param String cladeSecondary = ""
@param String cladeUniversity = ""

@template.layout.page(title = "Insect Species", content = @`
    @template.insects.cladeIntro(
        cladePreschool = cladePreschool,
        cladeElementary = cladeElementary,
        cladeSecondary = cladeSecondary,
        cladeUniversity = cladeUniversity)
    <nav class="insects-subnav" aria-label="Insects catalog sub-navigation">
        <a href="/insects/orders" aria-current="false">Order</a>
        <a href="/insects/families" aria-current="false">Family</a>
        <a href="/insects/genera" aria-current="false">Genus</a>
        <a href="/insects/species" aria-current="page">Species</a>
    </nav>
    <h1>Insect Species</h1>
    <p>Insect species documented at Oak Vista.</p>

    <div class="entity-grid">
        @for(var s : speciesPage.content())
            !{var images = imagesBySpecies.getOrDefault(s.name(), List.of());}
            !{var role = rolesBySpecies.get(s.name());}
            !{var guilds = role == null ? Set.<FunctionalGuild>of() : role.guilds();}
            !{var beneficial = role != null && role.beneficial();}
            !{var commonName = s.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(s.name().value());}
            !{var family = familyByName.get(s.familyName());}
            !{var genus = genusByName.get(s.genusName());}
            <article>
                <header>
                    <a href="/insects/${s.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                    @if(beneficial)
                        <span class="badge beneficial">beneficial</span>
                    @endif
                </header>
                <dl class="taxonomy">
                    <dt>fam.</dt>
                    <dd>${family != null ? family.family().value() : s.familyName().value()}</dd>
                    <dt>gen.</dt>
                    <dd><em>${genus != null ? genus.genus().value() : s.genusName().value()}</em></dd>
                    <dt>sp.</dt>
                    <dd><em>${s.epithet().value()}</em></dd>
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
                    @for(var guild : guilds)
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

    @template.components.pager(page = speciesPage, baseUrl = "/insects/species")

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
