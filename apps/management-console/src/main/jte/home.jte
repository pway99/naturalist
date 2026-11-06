!{var _homeAttrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();}
!{var _homeAnonymous = _homeAttrs == null || !Boolean.TRUE.equals(_homeAttrs.getAttribute("naturalistAuthenticated", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST));}
@template.layout.page(title = "The Backyard Naturalist", content = @`
    <h1>The Backyard Naturalist</h1>
    <p>A catalog of the living systems of one backyard: insects, plants, soil chemistry, the
        garden, and more. Photograph an insect, have a vision model identify it to the most
        specific rank the evidence supports, and file it in a catalog that ties together
        taxonomy, ecology, chemistry, and habitat.</p>
    @if(_homeAnonymous)
        <p>Anyone can browse the catalog. Identifying a photo needs an account.</p>
        <div class="home-actions">
            <a href="/login" role="button">Log in</a>
            <a href="/register" role="button" class="secondary outline">Join</a>
        </div>
    @endif
    <section>
        <h2>About this project</h2>
        <p>The subject is a hobby. The engineering is the point: a long-form study in
            Domain-Driven Design, hexagonal architecture, and structural enforcement, where every
            load-bearing decision is recorded and the build enforces the architecture.</p>
        <p>The inspiration is Gerald Durrell's <em>The Amateur Naturalist</em>, a guide to
            exploring the natural world close to home. The catalog follows its spirit: each entry
            tells the same truth at four levels of understanding, from preschool to university.</p>
        <p>It's a working personal project, not a product, built with Claude Code.
            <a href="https://github.com/pway99/naturalist">Read the source on GitHub</a>.</p>
    </section>
`)
