# The Amateur Naturalist

A modular monolith application for cataloging and observing natural systems at Oak Vista.

## Documentation

- [Architecture Decision Records](docs/adr/) — design rationale and system structure

## Web Console

The web console presents domain catalogs (insects, compounds, plants, etc.) with multi-level descriptions and observational tools.

The background image and opacity slider are automatically applied to:
- **All authenticated pages** — using the `page.jte` layout template (home page, insect catalog, detail pages, etc.)
- **Login page** — custom login template with the same background and opacity control

### Customizing the Background Image

The console background displays a fixed image with adjustable transparency via a slider in the header.

#### Default Login Credentials

- **Username:** `naturalist`
- **Password:** `durrell`

#### Changing the Background Image

The same background image is used on both the login page and all authenticated pages.

1. **Convert your image to JPEG** (web-friendly format):
   ```bash
   # macOS (sips)
   sips -s format jpeg input.heic --out output.jpg
   
   # Linux/Windows (ImageMagick)
   convert input.heic output.jpg
   ```

2. **Place the JPEG in the static images directory:**
   ```
   naturalist-web/console/src/main/resources/static/images/background.jpg
   ```
   Replace the existing `background.jpg`.

3. **Rebuild the console module:**
   ```bash
   mvn -pl naturalist-web/console clean package
   ```

4. **Restart the application** — the new background will load on next page refresh.

#### Adjusting Image Transparency

The image opacity slider is located at the bottom center of the page:
- **0% (left)** — image fully hidden (tan overlay only)
- **100% (right, default)** — image fully visible

The setting persists only during the current session and resets on page reload. To change the default opacity, modify the `value` attribute in `naturalist-web/console/src/main/jte/layout/page.jte`:

```html
<input type="range" id="bg-opacity-slider" min="0" max="100" value="100" class="opacity-slider">
```

Change `value="100"` to your preferred default (0–100).

#### CSS Styling

All styling variables are defined in the `:root` block of `naturalist-web/console/src/main/resources/static/css/naturalist.css` and can be adjusted in one place:

**Background Image:**
- `--image-opacity` — insect background image opacity (0 = hidden, 1 = fully visible). Controlled dynamically by the slider.

**Overlay:**
- `--bg-overlay-opacity` — tan overlay opacity (0 = hidden, 1 = fully opaque; default: 0.3)
- `--pico-background-color` — overlay color (default: tan `#EDE8DF`)

**Text and Content:**
- `--text-light-color` — text color for all paragraphs, links, and list items (default: `#FAF8F5`)
- `--text-shadow-strong` — shadow for headings (h1–h3)
- `--text-shadow-subtle` — shadow for body text
- `--content-background` — background overlay for the `<main>` element (default: semi-transparent dark `rgba(59, 50, 40, 0.6)`)

**Cards (Insect Species):**
- `--card-background` — card background color (default: earth brown `#A0826D`)
- `--card-header-background` — card header background color (default: dark earth `#6B5D52`)
- `--card-text-color` — text color on cards (default: light cream `#FAF8F5`)
- `--card-text-shadow` — shadow for text on cards

To adjust styling globally, edit only the `:root` variables — changes apply to all elements at once.

#### Applying Background to Additional Layout Templates

The background and opacity slider are built into the `page.jte` layout and automatically appear on all pages using it. If you create a new layout template that doesn't inherit from `page.jte`, you can add the background by:

1. **Copy the opacity slider component** from `naturalist-web/console/src/main/jte/layout/page.jte` (the `<div class="background-opacity-control">...</div>` block and its `<script>`)

2. **Include the CSS** — the `body::before` and `body::after` pseudo-elements in `naturalist.css` apply globally to all pages with `<body>` tags

3. **Ensure the layout includes the CSS files:**
   ```html
   <link rel="stylesheet" href="/css/pico.min.css">
   <link rel="stylesheet" href="/css/naturalist.css">
   ```

The pseudo-elements and opacity slider will then work on any page with those stylesheets loaded.
