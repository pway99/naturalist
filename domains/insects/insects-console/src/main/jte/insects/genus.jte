@import com.naturalist.observation.OrganismObservation
@import com.naturalist.insects.InsectCitationView
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectGenus
@import com.naturalist.observation.OrganismImage
@import com.naturalist.insects.InsectObservationId
@import com.naturalist.insects.InsectRankName
@import com.naturalist.insects.InsectImageId
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectSpeciesView
@import com.naturalist.insects.InsectTaxonView
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import com.naturalist.insects.InsectFeatureView
@import com.naturalist.library.console.GlossaryLinker
@import java.util.List
@import java.util.Map

@param InsectGenus genus
@param InsectFamily family
@param InsectOrder order
@param List<InsectTaxonView> children = List.of()
@param InsectCitationView citations = null
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param CladeTrail cladeTrail = null
@param List<AncestorIntro> ancestorIntros = List.of()
@param InsectFeatureView features = null
@param GlossaryLinker glossaryLinker = GlossaryLinker.none()
@param List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> images = List.of()
@param Map<InsectImageId, OrganismObservation> observations = Map.of()

!{var commonName = genus.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(genus.genus().value());}
<%-- The CSRF token is read the same way layout/page.jte reads it — plain request
     attributes published by NaturalistHeaderInterceptor (management-console only)
     — rather than a CsrfToken-typed @param, so this template stays compilable
     against classpaths lacking Spring Security (see page.jte:27-33). --%>
!{var _attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();}
!{var _csrfParam = _attrs != null ? (String) _attrs.getAttribute("naturalistCsrfParam", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST) : null;}
!{var _csrfToken = _attrs != null ? (String) _attrs.getAttribute("naturalistCsrfToken", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST) : null;}

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
    @template.insects.features(features = features, glossaryLinker = glossaryLinker)
    @template.insects.observationGallery(
        images = images,
        observations = observations,
        alt = commonName,
        notesAction = "/insects/" + genus.name().value() + "/notes",
        returnPath = "/insects/genera/" + genus.name().value(),
        csrfParam = _csrfParam,
        csrfToken = _csrfToken)

    <section>
        <h2>Species in this genus</h2>
        @if(children.isEmpty())
            <p><em>No species catalogued under this genus yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var child : children)
                    @if(child instanceof InsectSpeciesView sv)
                        !{var s = sv.species();}
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
                            !{var childImages = sv.images().stream().toList();}
                            @if(!childImages.isEmpty())
                                @template.insects.cardImages(images = childImages, linkUrl = "/insects/" + s.name().value(), alt = speciesCommonName)
                            @endif
                        </article>
                    @endif
                @endfor
            </div>
        @endif
    </section>
`)
