@import com.naturalist.insects.InsectCitationView
@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param InsectFamily family
@param InsectOrder order
@param List<InsectGenus> genera
@param InsectCitationView citations = null
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param List<AncestorIntro> ancestorIntros = List.of()

!{var commonName = family.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(family.family().value());}

@template.layout.page(title = family.family().value(), content = @`
    @template.insects.nav(active = "family", breadcrumb = breadcrumb)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    <h1>${commonName}</h1>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @template.insects.citations(citations = citations)

    <section>
        <h2>Genera in this family</h2>
        @if(genera.isEmpty())
            <p><em>No genera catalogued under this family yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var g : genera)
                    !{var genusCommonName = g.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(g.name().value());}
                    <article>
                        <header>
                            <a href="/insects/genera/${g.name().value()}">
                                <strong>${genusCommonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>genus</dt>
                            <dd><em>${g.genus().value()}</em></dd>
                        </dl>
                        !{var images = gallery.forEntity(g.name()).stream().toList();}
                        @if(!images.isEmpty())
                            @template.insects.cardImages(images = images, linkUrl = "/insects/genera/" + g.name().value(), alt = genusCommonName)
                        @endif
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
