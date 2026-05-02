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
    <link rel="stylesheet" href="/css/naturalist.css?v=2">
    <script src="/js/htmx.min.js" defer></script>
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
                    <li class="primary-nav-more">
                        <details>
                            <summary>More <span class="primary-nav-caret" aria-hidden="true">&#9662;</span></summary>
                            <ul class="primary-nav-overflow">
                                <%-- Add overflow domain links here as the catalog grows.
                                     Items appear inside a parchment popover; the
                                     "More" disclosure auto-hides while this list is empty. --%>
                            </ul>
                        </details>
                    </li>
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
    <div class="background-opacity-control" title="Image Opacity">
        <input type="range" id="bg-opacity-slider" min="0" max="100" value="100" class="opacity-slider">
        <div id="opacity-display">100%</div>
    </div>
    <script>
        const slider = document.getElementById('bg-opacity-slider');
        const display = document.getElementById('opacity-display');

        function updateOpacity() {
            const value = slider.value;
            const imageOpacity = value / 100;
            document.documentElement.style.setProperty('--image-opacity', imageOpacity);
            display.textContent = value + '%';
        }

        slider.addEventListener('input', updateOpacity);
        updateOpacity();
    </script>
</body>
</html>
