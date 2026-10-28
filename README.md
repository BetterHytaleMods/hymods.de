# hymods.de

The Hymods website: a start page with one card per mod and a page per mod, so far
[FlexiisGoggles](https://hymods.de/flexiisgoggles/). Hand-written HTML and one stylesheet, no build
step, system fonts, nothing loaded from other servers (each page sets a Content-Security-Policy that
only allows its own files).

```
public/                       what the web server serves
  index.html                  start page
  flexiisgoggles/index.html   the mod's page; rendered pictures in img/, screenshots in shots/,
                              link preview og.jpg
  assets/                     site.css, back.js (back arrow for visitors from fherrmann.com),
                              apple-touch-icon.png, og.jpg (link preview of the start page)
  404.html  robots.txt  sitemap.xml  favicon.ico
tools/Showcase.java           renders the pictures from the mod's own models (as released)
tools/MirrorShowcase.java     renders the pictures of what 0.2.0 brings, on top of Showcase
tools/render.sh               runs both and converts the result (WebP + PNG fallback, favicons)
deploy/                       server setup (once) and the update script
```

## Preview

```bash
cd public && python3 -m http.server 8000     # http://localhost:8000
```

Links are root-relative (`/assets/…`), so serve `public/` as the root rather than opening the files.

## Pictures

Every picture is rendered from the mod's models and textures with the mod's own renderer (`dev/art` in
its repository), each from its own ref of the mod: what is released by `Showcase.java` (default: tag
`v0.1.0`; 0.1.1 changed no model), what 0.2.0 brings by `MirrorShowcase.java` (default: branch
`adventure`). MirrorShowcase also poses the creatures with the mod's tools (`Bunker.Assets`,
`Taken.figure`), so it needs a ref that has them.

```bash
tools/render.sh                    # or: tools/render.sh v0.1.1 v0.2.0
```

Needs Java 25, ImageMagick, `cwebp`, the mod repository (`MOD_REPO`, default
`~/Server-Projects/Hytale/hytale-flexiisgoggles`) and the game's `Assets.zip` (`HYTALE_ASSETS`,
default: the installed launcher's). The game supplies the display head the goggles are shown on and
the stone of the ore blocks. `img/logo.png` is the release logo and is not rendered; the favicons
are made from it.

## Screenshot slots

Seven figures on the mod page show renders until in-game screenshots replace them. Each has a
`data-slot` attribute and a comment above it.

| Slot | Section | Shows now |
|---|---|---|
| `zoom-in-action` | Zoom Goggles | `worn-zoom-4` |
| `night-vision-before-after` | Night Vision Goggles | `cave-dark` and `cave-lit` |
| `goggles-on-player` | UltraGoggles | `worn-ultra` |
| `bench-in-world` | Optician's Bench | `bench-tier5-large` |
| `quartz-ore-in-cave` | Quartz & Lumenite | `ore-quartz` |
| `lumenite-revealed` | Quartz & Lumenite | `ore-lumenite-hidden` and `ore-lumenite-revealed` |
| `settings-page` | Settings page | a sketch in HTML (`div.mock`) |

To fill one, put a WebP and a PNG of the screenshot into `public/flexiisgoggles/shots/` (not
`img/`: `tools/render.sh` empties that folder):

```bash
mkdir -p public/flexiisgoggles/shots
magick shot.png -resize '1600x1600>' -strip public/flexiisgoggles/shots/zoom.png
cwebp -q 85 public/flexiisgoggles/shots/zoom.png -o public/flexiisgoggles/shots/zoom.webp
magick public/flexiisgoggles/shots/zoom.png -format '%w %h\n' info:    # for width/height
```

then put this in place of the slot's `<picture>` (for the two pairs, one per side; for the settings
page, in place of the sketch) and add the class `shot` to the element with the `data-slot`, so the
screenshot fills the frame:

```html
<picture>
  <source type="image/webp" srcset="/flexiisgoggles/shots/zoom.webp">
  <img src="/flexiisgoggles/shots/zoom.png" width="1600" height="900" loading="lazy" decoding="async" alt="…">
</picture>
```

## Another mod

Copy the card in `public/index.html`, make `public/<mod>/index.html` (the FlexiisGoggles page is the
pattern, `site.css` has everything it uses) and add the page to `sitemap.xml`.

## Deploy

The web server serves `public/` from a clone of this repository in `/opt/hymods.de/app`.

- Once: `deploy/setup-server.sh` (the header says how to run it). It clones the repository, points
  nginx at the clone, keeps certbot's webroot as it is and installs the update script.
- Updates: commit and push, then on the server `~/scripts/update-hymods.sh` (`git pull --ff-only`,
  no sudo). Only what is pushed arrives.
