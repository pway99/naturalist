@import com.naturalist.insects.InsectViews
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.AncestorIntro
@import com.naturalist.insects.BreadcrumbSegment
@import com.naturalist.insects.CladeTrail
@import com.naturalist.library.GlossaryLinker
@import java.util.List

@param InsectViews.RankPage page
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param CladeTrail cladeTrail = null
@param List<AncestorIntro> ancestorIntros = List.of()
@param GlossaryLinker glossaryLinker = GlossaryLinker.none()

!{var insect = page.insect();}
!{var children = insect.children();}
!{var citations = insect.citations();}
!{var features = insect.features();}
!{var images = insect.observations().stream().toList();}
!{var observations = page.viewer().ownObservationByImage();}
!{var family = insect.family().info();}
!{var commonName = family.displayName();}
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
    @template.insects.features(features = features, glossaryLinker = glossaryLinker)
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
        @if(children.isEmpty())
            <p><em>No genera catalogued under this family yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var child : children)
                    @if(child instanceof InsectGenus gv)
                        !{var g = gv.info();}
                        !{var genusCommonName = g.displayName();}
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
                            !{var childImages = gv.images().stream().toList();}
                            @if(!childImages.isEmpty())
                                @template.insects.cardImages(images = childImages, linkUrl = "/insects/genera/" + g.name().value(), alt = genusCommonName)
                            @endif
                        </article>
                    @endif
                @endfor
            </div>
        @endif
    </section>
`)
