#!/usr/bin/env bash
set -euo pipefail

# Renders the FlexiisGoggles pictures of hymods.de from the mod's own models and turns them into
# web files: WebP plus a PNG fallback in public/flexiisgoggles/img/ (which it empties first, except
# for logo.png), the link previews as JPEG, and the favicons from the logo.
#
# Two states of the mod, each from its own ref: what is released (Showcase.java) and what 0.2.0
# brings (MirrorShowcase.java: the mirror dimension, its creatures, the blight, the companion;
# AdventureShowcase.java: Wren Hollow, the story adventure, its buildings and people).
#
#     tools/render.sh                       # released from tag v0.1.0, 0.2.0 from branch adventure
#     tools/render.sh v0.1.1 v0.2.0         # other tags or commits of the mod
#
# Needs Java 25, ImageMagick (magick), cwebp, the mod's repository and the game's Assets.zip:
#
#     MOD_REPO        default ~/Server-Projects/Hytale/hytale-flexiisgoggles
#     HYTALE_ASSETS   default: the Assets.zip of the installed Hytale launcher
#
# The mod is read with `git archive`, so its working tree (unfinished work) does not matter.

released="${1:-v0.1.0}"
upcoming="${2:-adventure}"
repo="$(cd "$(dirname "$0")/.." && pwd)"
mod="${MOD_REPO:-$HOME/Server-Projects/Hytale/hytale-flexiisgoggles}"
assets="${HYTALE_ASSETS:-$HOME/Library/Application Support/Hytale/install/release/package/game/latest/Assets.zip}"
img="$repo/public/flexiisgoggles/img"

if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 25)"
fi
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}"

for tool in magick cwebp git; do
    command -v "$tool" >/dev/null || { echo "missing: $tool" >&2; exit 1; }
done
[[ -f "$assets" ]] || { echo "Assets.zip not found: $assets (set HYTALE_ASSETS)" >&2; exit 1; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

echo "=== mod $released (released)"
mkdir -p "$work/released" "$work/upcoming"
git -C "$mod" archive "$released" dev/art src/main/resources/Common | tar -x -C "$work/released"
"${java_bin}javac" -d "$work/released-classes" "$work"/released/dev/art/*.java "$repo/tools/Showcase.java"
"${java_bin}java" -Djava.awt.headless=true -cp "$work/released-classes" Showcase "$work/released" "$assets" "$work/out"

echo "=== mod $upcoming (0.2.0)"
git -C "$mod" archive "$upcoming" dev/art src/main/resources | tar -x -C "$work/upcoming"
"${java_bin}javac" -d "$work/upcoming-classes" "$work"/upcoming/dev/art/*.java "$repo/tools/Showcase.java" \
    "$repo/tools/MirrorShowcase.java" "$repo/tools/AdventureShowcase.java"
# the mod's tools find its files relative to the checkout, and the game through HYTALE_ASSETS
for showcase in MirrorShowcase AdventureShowcase; do
    (cd "$work/upcoming" && HYTALE_ASSETS="$assets" "${java_bin}java" -Djava.awt.headless=true -cp "$work/upcoming-classes" \
        "$showcase" "$work/upcoming" "$assets" "$work/out")
done

echo "=== convert"
# img/ holds only rendered pictures (and the logo); screenshots go to shots/
mkdir -p "$img"
find "$img" -type f ! -name 'logo.png' -delete
for png in "$work"/out/*.png; do
    name="$(basename "$png" .png)"
    case "$name" in
        og-flexiisgoggles) magick "$png" -strip -quality 88 "$repo/public/flexiisgoggles/og.jpg" ;;
        og-hymods) magick "$png" -strip -quality 88 "$repo/public/assets/og.jpg" ;;
        *)
            magick "$png" -strip -define png:compression-level=9 "$img/$name.png"
            cwebp -quiet -q 90 -alpha_q 100 -sharp_yuv -m 6 "$png" -o "$img/$name.webp"
            ;;
    esac
done

# The logo was drawn for the release and stays as it is; the rest is made from it.
logo="$img/logo.png"
cwebp -quiet -q 92 -alpha_q 100 -sharp_yuv -m 6 "$logo" -o "$img/logo.webp"
magick "$logo" -strip -resize 180x180 "$repo/public/assets/apple-touch-icon.png"
magick "$logo" -gravity center -crop 344x344+0+0 +repage \
    \( -clone 0 -resize 16x16 \) \( -clone 0 -resize 32x32 \) \( -clone 0 -resize 48x48 \) \
    -delete 0 "$repo/public/favicon.ico"

echo "done: $(ls "$img" | wc -l | tr -d ' ') files in public/flexiisgoggles/img"
