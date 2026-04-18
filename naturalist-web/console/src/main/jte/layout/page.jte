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
</body>
</html>
