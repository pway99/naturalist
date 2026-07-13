@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectFamilyName
@import com.naturalist.insects.InsectFunctionalRole
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectGenusName
@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.InsectSpeciesName
@import com.naturalist.insects.FunctionalGuild
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import com.naturalist.data.Page
@import java.util.List
@import java.util.Map
@import java.util.Set

@param Page<InsectSpecies> speciesPage
@param List<InsectSpecies> speciesList = List.of()
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param Map<InsectSpeciesName, InsectFunctionalRole> rolesBySpecies = Map.of()
@param Map<InsectFamilyName, InsectFamily> familyByName = Map.of()
@param Map<InsectGenusName, InsectGenus> genusByName = Map.of()
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param CladeTrail cladeTrail = null
@param List<AncestorIntro> ancestorIntros = List.of()
@param boolean mine = false

@template.layout.page(title = "Insect Species", content = @`
    @template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    <h1>Insect Species</h1>
    <p>Insect species documented at Oak Vista.</p>

    <div class="entity-grid">
        @for(var s : speciesList)
            !{var images = gallery.forEntity(s.name()).stream().toList();}
            !{var role = rolesBySpecies.get(s.name());}
            !{var guilds = role == null ? Set.<FunctionalGuild>of() : role.guilds();}
            !{var beneficial = role != null && role.beneficial();}
            !{var commonName = s.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(s.name().value());}
            !{var genus = genusByName.get(s.genusName());}
            !{var family = genus != null ? familyByName.get(genus.familyName()) : null;}
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
                    <dd>${family != null ? family.family().value() : (genus != null ? genus.familyName().value() : "—")}</dd>
                    <dt>gen.</dt>
                    <dd><em>${genus != null ? genus.genus().value() : s.genusName().value()}</em></dd>
                    <dt>sp.</dt>
                    <dd><em>${s.epithet().value()}</em></dd>
                </dl>
                @if(!images.isEmpty())
                    @template.insects.cardImages(images = images, linkUrl = "/insects/" + s.name().value(), alt = commonName)
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
`)
