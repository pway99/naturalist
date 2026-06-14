@import com.naturalist.insects.InsectCitationView
@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param InsectOrder order
@param List<InsectFamily> families
@param InsectCitationView citations = null
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param List<AncestorIntro> ancestorIntros = List.of()

!{var commonName = order.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(order.order().value());}

@template.layout.page(title = order.order().value(), content = @`
    @template.insects.nav(active = "order", breadcrumb = breadcrumb)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    <h1>${commonName}</h1>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @template.insects.citations(citations = citations)

    <section>
        <h2>Families in this order</h2>
        @if(families.isEmpty())
            <p><em>No families catalogued under this order yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var f : families)
                    !{var familyCommonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.name().value());}
                    <article>
                        <header>
                            <a href="/insects/families/${f.name().value()}">
                                <strong>${familyCommonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>family</dt>
                            <dd>${f.family().value()}</dd>
                        </dl>
                        !{var images = gallery.forEntity(f.name()).stream().toList();}
                        @if(!images.isEmpty())
                            @template.insects.cardImages(images = images, linkUrl = "/insects/families/" + f.name().value(), alt = familyCommonName)
                        @endif
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
