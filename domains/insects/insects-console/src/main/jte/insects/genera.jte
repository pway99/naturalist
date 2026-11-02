@import com.naturalist.insects.InsectViews
@import com.naturalist.insects.AncestorIntro
@import com.naturalist.insects.BreadcrumbSegment
@import com.naturalist.insects.CladeTrail
@import com.naturalist.data.Page
@import java.util.List

@param Page<InsectViews.RankCard> cards
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param CladeTrail cladeTrail = null
@param List<AncestorIntro> ancestorIntros = List.of()

@template.layout.page(title = "Insect Genera", content = @`
    @template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    <h1>Insect Genera</h1>
    <p>Genus-rank records in the Oak Vista catalog.</p>

    <div class="entity-grid">
        @for(var card : cards.content())
            !{var commonName = card.displayName();}
            <article>
                <header>
                    <a href="/insects/genera/${card.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>family</dt>
                    <dd><a href="/insects/families/${card.parentName().value()}">${card.parentLabel()}</a></dd>
                    <dt>genus</dt>
                    <dd><em>${card.scientificName()}</em></dd>
                </dl>
                !{var images = card.images().stream().toList();}
                @if(!images.isEmpty())
                    @template.insects.cardImages(images = images, linkUrl = "/insects/genera/" + card.name().value(), alt = commonName)
                @endif
            </article>
        @endfor
    </div>

    @template.components.pager(page = cards, baseUrl = "/insects/genera")
`)
