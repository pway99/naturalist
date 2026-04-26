@param String title = "The Amateur Naturalist"
@param gg.jte.Content content

<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title} — Console</title>
    <link rel="stylesheet" href="/css/pico.min.css">
    <link rel="stylesheet" href="/css/naturalist.css">
    <script src="/js/htmx.min.js" defer></script>
</head>
<body>
    <header class="container">
        <nav>
            <ul><li><strong>The Amateur Naturalist</strong></li></ul>
            <ul>
                <li><a href="/">Home</a></li>
                <li><a href="/chemistry">Chemistry</a></li>
                <li><a href="/insects">Insects</a></li>
            </ul>
        </nav>
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
