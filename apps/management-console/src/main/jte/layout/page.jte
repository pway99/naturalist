@param String title = "The Amateur Naturalist"
@param gg.jte.Content content
@param String query = ""

<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title} — Console</title>
    <link rel="stylesheet" href="/css/pico.min.css">
    <%-- Bump ?v= when naturalist.css ships visible changes so browsers
         don't keep serving the previous cached version. --%>
    <link rel="stylesheet" href="/css/naturalist.css?v=25">
    <script src="/js/htmx.min.js" defer></script>
    <%-- Rotates stacked images on .card-image[data-rotate] tiles across all
         catalog cards. No-op on pages without rotating cards. --%>
    <script src="/js/card-image-rotate.js" defer></script>
</head>
<body>
<header class="site-header">
    <div class="container site-header-bar">
        <a class="site-brand" href="/">The Amateur Naturalist</a>
        <div class="site-search">
            @template.components.searchBox(query = query)
        </div>
        <%-- The shared header reads plain request attributes published by
             NaturalistHeaderInterceptor (management-console only). page.jte is
             compiled by every domain-console template test against classpaths
             lacking Spring-Security, the console.auth package, AND the jakarta
             servlet-api. So it must reference only the RequestAttributes
             interface (spring-web), never ServletRequestAttributes.getRequest()
             — that would drag in jakarta.servlet.http.HttpServletRequest. --%>
        !{var _attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();}
        @if(_attrs != null && Boolean.TRUE.equals(_attrs.getAttribute("naturalistAuthenticated", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST)))
            !{var _name = (String) _attrs.getAttribute("naturalistDisplayName", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            !{var _slug = (String) _attrs.getAttribute("naturalist.currentNaturalistName", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            !{var _csrfParam = (String) _attrs.getAttribute("naturalistCsrfParam", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            !{var _csrfValue = (String) _attrs.getAttribute("naturalistCsrfToken", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            !{var _insectSection = Boolean.TRUE.equals(_attrs.getAttribute("insectSection", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST));}
            !{var _lensOn = Boolean.TRUE.equals(_attrs.getAttribute("collectionLens", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST));}
            !{var _returnTo = (String) _attrs.getAttribute("requestUri", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            <div class="site-actions">
                @if(_insectSection && _slug != null)
                    <form method="post" action="/insects/collection-lens" class="lens-form">
                        @if(_csrfParam != null)
                            <input type="hidden" name="${_csrfParam}" value="${_csrfValue}">
                        @endif
                        <input type="hidden" name="return" value="${_returnTo}">
                        <button type="submit" name="on" value="${_lensOn ? "false" : "true"}"
                                class="lens-chip ${_lensOn ? "active" : ""}"
                                aria-pressed="${_lensOn ? "true" : "false"}">
                            ${_lensOn ? "✓ My collection" : "My collection"}
                        </button>
                    </form>
                @endif
                <div class="site-user">
                    @if(_name != null)
                        <span class="site-user-badge">${_name.substring(0, 1)}</span>
                        <span class="site-user-name">${_name}</span>
                    @endif
                    <form method="post" action="/logout" class="site-logout">
                        @if(_csrfParam != null)
                            <input type="hidden" name="${_csrfParam}" value="${_csrfValue}">
                        @endif
                        <button type="submit">Log out</button>
                    </form>
                </div>
            </div>
        @endif
    </div>
    <div class="container site-nav-row">
        <nav class="primary-nav" aria-label="Primary navigation">
            <ul class="primary-nav-links">
                <li><a href="/">Home</a></li>
                <li><a href="/chemistry">Chemistry</a></li>
                <li><a href="/insects">Insects</a></li>
                <li><a href="/plants">Plants</a></li>
                <li><a href="/clades">Tree of Life</a></li>
                <li><a href="/concepts">Concepts</a></li>
                <li><a href="/citations">Citations</a></li>
            </ul>
        </nav>
    </div>
</header>
<main class="container">
    ${content}
</main>
<footer class="container">
    <small>Management Console</small>
</footer>
</body>
</html>
