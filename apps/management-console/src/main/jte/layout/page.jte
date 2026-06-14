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
    <link rel="stylesheet" href="/css/naturalist.css?v=19">
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
