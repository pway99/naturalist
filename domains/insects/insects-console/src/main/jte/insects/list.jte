@import com.naturalist.insects.InsectViews
@import com.naturalist.insects.FunctionalGuild
@import com.naturalist.insects.AncestorIntro
@import com.naturalist.insects.BreadcrumbSegment
@import com.naturalist.insects.CladeTrail
@import com.naturalist.data.Page
@import java.util.List
@import java.util.Set

@param Page<InsectViews.SpeciesCard> cards
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
        @for(var card : cards.content())
            !{var s = card.species();}
            !{var images = card.images().stream().toList();}
            !{var role = card.role();}
            !{var guilds = role == null ? Set.<FunctionalGuild>of() : role.guilds();}
            !{var beneficial = role != null && role.beneficial();}
            !{var commonName = card.displayName();}
            <article>
                <header>
                    <a href="/insects/${s.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                    @if(beneficial)
                        <span class="badge beneficial">beneficial</span>
                    @endif
                </header>
                @if(card.collected())
                    <p class="collected-badge">✓ In your collection</p>
                @endif
                <dl class="taxonomy">
                    <dt>fam.</dt>
                    <dd>${card.familyLabel()}</dd>
                    <dt>gen.</dt>
                    <dd><em>${card.genusLabel()}</em></dd>
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

    @if(mine && cards.content().isEmpty())
        <p class="collection-empty">No insects in your collection yet — turn off "My collection" and go make some observations.</p>
    @endif

    @template.components.pager(page = cards, baseUrl = "/insects/species")
`)
