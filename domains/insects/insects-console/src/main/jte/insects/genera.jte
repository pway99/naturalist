@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectFamilyName
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.data.Page
@import java.util.List
@import java.util.Map

@param Page<InsectGenus> genusPage
@param Map<InsectFamilyName, InsectFamily> familyByName
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param List<AncestorIntro> ancestorIntros = List.of()

@template.layout.page(title = "Insect Genera", content = @`
    @template.insects.nav(active = "genus", breadcrumb = breadcrumb)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    <h1>Insect Genera</h1>
    <p>Genus-rank records in the Oak Vista catalog.</p>

    <div class="entity-grid">
        @for(var g : genusPage.content())
            !{var commonName = g.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(g.name().value());}
            !{var family = familyByName.get(g.familyName());}
            <article>
                <header>
                    <a href="/insects/genera/${g.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>family</dt>
                    <dd><a href="/insects/families/${g.familyName().value()}">${family.family().value()}</a></dd>
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

    @template.components.pager(page = genusPage, baseUrl = "/insects/genera")
`)
