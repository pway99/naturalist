@import com.naturalist.insects.InsectCitationView
@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import java.util.List

@param InsectGenus genus
@param InsectFamily family
@param InsectOrder order
@param List<InsectSpecies> species
@param InsectCitationView citations = null
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param CladeTrail cladeTrail = null
@param List<AncestorIntro> ancestorIntros = List.of()

!{var commonName = genus.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(genus.genus().value());}

@template.layout.page(title = genus.genus().value(), content = @`
    @template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    <h1>${commonName}</h1>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @template.insects.citations(citations = citations)

    <section>
        <h2>Species in this genus</h2>
        @if(species.isEmpty())
            <p><em>No species catalogued under this genus yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var s : species)
                    !{var speciesCommonName = s.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(s.name().value());}
                    <article>
                        <header>
                            <a href="/insects/${s.name().value()}">
                                <strong>${speciesCommonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>species</dt>
                            <dd><em>${s.epithet().value()}</em></dd>
                        </dl>
                        !{var images = gallery.forEntity(s.name()).stream().toList();}
                        @if(!images.isEmpty())
                            @template.insects.cardImages(images = images, linkUrl = "/insects/" + s.name().value(), alt = speciesCommonName)
                        @endif
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
