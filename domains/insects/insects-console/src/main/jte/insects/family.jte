@import com.naturalist.insects.FieldObservation
@import com.naturalist.insects.InsectCitationView
@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectImage
@import com.naturalist.insects.InsectImageId
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import com.naturalist.insects.console.FeatureGroup
@import java.util.List
@import java.util.Map

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
@param CladeTrail cladeTrail = null
@param List<AncestorIntro> ancestorIntros = List.of()
@param List<FeatureGroup> featureGroups = List.of()
@param List<InsectImage> images = List.of()
@param Map<InsectImageId, FieldObservation> observations = Map.of()

!{var commonName = family.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(family.family().value());}
<%-- The CSRF token is read the same way layout/page.jte reads it — plain request
     attributes published by NaturalistHeaderInterceptor (management-console only)
     — rather than a CsrfToken-typed @param, so this template stays compilable
     against classpaths lacking Spring Security (see page.jte:27-33). --%>
!{var _attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();}
!{var _csrfParam = _attrs != null ? (String) _attrs.getAttribute("naturalistCsrfParam", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST) : null;}
!{var _csrfToken = _attrs != null ? (String) _attrs.getAttribute("naturalistCsrfToken", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST) : null;}

@template.layout.page(title = family.family().value(), content = @`
    @template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    <h1>${commonName}</h1>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @template.insects.citations(citations = citations)
    @template.insects.features(featureGroups = featureGroups)
    @template.insects.observationGallery(
        images = images,
        observations = observations,
        alt = commonName,
        notesAction = "/insects/" + family.name().value() + "/notes",
        returnPath = "/insects/families/" + family.name().value(),
        csrfParam = _csrfParam,
        csrfToken = _csrfToken)

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
                        !{var childImages = gallery.forEntity(g.name()).stream().toList();}
                        @if(!childImages.isEmpty())
                            @template.insects.cardImages(images = childImages, linkUrl = "/insects/genera/" + g.name().value(), alt = genusCommonName)
                        @endif
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
