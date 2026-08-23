@import com.naturalist.observation.OrganismObservation
@import com.naturalist.insects.InsectCitationView
@import com.naturalist.insects.InsectFamilyView
@import com.naturalist.observation.OrganismImage
@import com.naturalist.insects.InsectObservationId
@import com.naturalist.insects.InsectRankName
@import com.naturalist.insects.InsectImageId
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectTaxonView
@import com.naturalist.insects.AncestorIntro
@import com.naturalist.insects.BreadcrumbSegment
@import com.naturalist.insects.CladeTrail
@import com.naturalist.insects.InsectFeatureView
@import com.naturalist.library.console.GlossaryLinker
@import java.util.List
@import java.util.Map

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

!{var commonName = order.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(order.order().value());}
<%-- The CSRF token is read the same way layout/page.jte reads it — plain request
     attributes published by NaturalistHeaderInterceptor (management-console only)
     — rather than a CsrfToken-typed @param, so this template stays compilable
     against classpaths lacking Spring Security (see page.jte:27-33). --%>
!{var _attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();}
!{var _csrfParam = _attrs != null ? (String) _attrs.getAttribute("naturalistCsrfParam", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST) : null;}
!{var _csrfToken = _attrs != null ? (String) _attrs.getAttribute("naturalistCsrfToken", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST) : null;}

@template.layout.page(title = order.order().value(), content = @`
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
        notesAction = "/insects/" + order.name().value() + "/notes",
        returnPath = "/insects/orders/" + order.name().value(),
        csrfParam = _csrfParam,
        csrfToken = _csrfToken)

    <section>
        <h2>Families in this order</h2>
        @if(children.isEmpty())
            <p><em>No families catalogued under this order yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var child : children)
                    @if(child instanceof InsectFamilyView fv)
                        !{var f = fv.family();}
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
                            !{var childImages = fv.images().stream().toList();}
                            @if(!childImages.isEmpty())
                                @template.insects.cardImages(images = childImages, linkUrl = "/insects/families/" + f.name().value(), alt = familyCommonName)
                            @endif
                        </article>
                    @endif
                @endfor
            </div>
        @endif
    </section>
`)
