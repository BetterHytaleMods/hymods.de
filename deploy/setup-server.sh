#!/usr/bin/env bash
set -euo pipefail

# One-time move of hymods.de from the hand-copied /var/www/hymods.de/public to a clone of this
# repository in /opt/hymods.de/app, kept up to date with ~/scripts/update-hymods.sh.
#
# Run it on the server as the user who owns the sites (not with sudo; it asks for sudo itself).
# -t is needed so sudo can ask for the password. The first time, the clone does not exist yet:
#
#     scp deploy/setup-server.sh <host>:/tmp/setup-hymods.sh
#     ssh -t <host> 'bash /tmp/setup-hymods.sh'
#
# Afterwards it is in the clone, and running it again changes nothing that is already done:
#
#     ssh -t <host> '/opt/hymods.de/app/deploy/setup-server.sh'
#
# What it changes:
#   - /opt/hymods.de (owned by you) with the clone in /opt/hymods.de/app
#   - nginx: the HTTPS server of hymods.de serves /opt/hymods.de/app/public and answers missing
#     pages with /404.html. sites-enabled/hymods.de, so far a plain copy of the file in
#     sites-available, becomes a symlink to it.
#   - ~/scripts/update-hymods.sh
# What it leaves alone:
#   - /var/www/hymods.de: certbot renews the certificate through it (webroot), and the HTTP server
#     keeps answering the ACME challenge from there. The old index.html stays where it is; a copy
#     of the folder goes to ~/hymods.de-www.bak-<time>.tar.gz.
#   - the certificate and certbot's renewal settings.
#
# Back to the old page: copy ~/nginx-hymods.de.bak-<time> (the script prints the name) over
# /etc/nginx/sites-available/hymods.de, then `sudo nginx -t && sudo systemctl reload nginx`.

REPO_URL="https://github.com/BetterHytaleMods/hymods.de.git"
BASE="/opt/hymods.de"
APP="$BASE/app"
ROOT="$APP/public"
OLD_ROOT="/var/www/hymods.de/public"
AVAILABLE="/etc/nginx/sites-available/hymods.de"
ENABLED="/etc/nginx/sites-enabled/hymods.de"
STAMP="$(date +%Y%m%d-%H%M%S)"
ME="$(id -un)"

step() { echo; echo "=== $* ==="; }
fail() { echo "ERROR: $*" >&2; exit 1; }

# Check the configuration and reload, waiting if nginx is briefly stopped (certbot does that
# during renewals).
nginx_apply() {
    sudo nginx -t
    for attempt in $(seq 1 30); do
        if systemctl is-active --quiet nginx; then
            sudo systemctl reload nginx
            return 0
        fi
        [[ $attempt -eq 1 ]] && echo "    nginx is not running right now (certbot?). Waiting ..."
        sleep 2
    done
    fail "nginx has not been active for 60 s. See: systemctl status nginx"
}

# What hymods.de answers on this machine, without going out to the internet and back.
local_get() {
    curl -s --max-time 10 --resolve "hymods.de:$1:127.0.0.1" "${@:2}"
}

[[ $EUID -eq 0 ]] && fail "Do not run this with sudo; it calls sudo itself where needed."
for tool in git python3 curl tar; do
    command -v "$tool" >/dev/null || fail "missing: $tool"
done
[[ -f "$AVAILABLE" ]] || fail "$AVAILABLE is missing."

step "1/7 $BASE"
if [[ ! -d "$BASE" ]]; then
    sudo install -d -o "$ME" -g "$(id -gn)" -m 755 "$BASE"
    echo "    created"
elif [[ "$(stat -c %U "$BASE")" != "$ME" ]]; then
    sudo chown "$ME:$(id -gn)" "$BASE"
    echo "    now owned by $ME"
else
    echo "    there"
fi

step "2/7 Clone in $APP"
if [[ -d "$APP/.git" ]]; then
    origin="$(git -C "$APP" remote get-url origin)"
    [[ "$origin" == "$REPO_URL" || "$origin" == "git@github.com:BetterHytaleMods/hymods.de.git" ]] \
        || fail "$APP is a clone of $origin, not of $REPO_URL."
    git -C "$APP" pull --ff-only
elif [[ -e "$APP" ]]; then
    fail "$APP exists but is not a git clone. Look at it first."
else
    git clone "$REPO_URL" "$APP"
fi
for page in index.html 404.html flexiisgoggles/index.html; do
    [[ -f "$ROOT/$page" ]] || fail "$ROOT/$page is missing."
done
# nginx reads the files as www-data
chmod o+x "$BASE" "$APP"
chmod -R o+rX "$ROOT"

step "3/7 Copy of the old page"
old_copy="$(compgen -G "$HOME/hymods.de-www.bak-*.tar.gz" | head -1 || true)"
if [[ -n "$old_copy" ]]; then
    echo "    already there: $old_copy"
else
    tar -czf "$HOME/hymods.de-www.bak-$STAMP.tar.gz" -C /var/www hymods.de
    echo "    $HOME/hymods.de-www.bak-$STAMP.tar.gz"
fi

step "4/7 sites-enabled/hymods.de"
# Must be the same as sites-available before that one is edited, or the edit would not count.
if [[ -L "$ENABLED" ]]; then
    [[ "$(readlink -f "$ENABLED")" == "$AVAILABLE" ]] || fail "$ENABLED points to $(readlink -f "$ENABLED"), not to $AVAILABLE."
    echo "    symlink to $AVAILABLE"
    replace_enabled=0
elif [[ -f "$ENABLED" ]]; then
    cmp -s "$ENABLED" "$AVAILABLE" || fail "$ENABLED is a file of its own that differs from $AVAILABLE. Merge them by hand first."
    echo "    plain copy of $AVAILABLE, becomes a symlink"
    replace_enabled=1
else
    fail "$ENABLED is missing; hymods.de is not switched on."
fi

step "5/7 nginx: serve $ROOT"
if grep -qE "^    root $ROOT;$" "$AVAILABLE" && grep -qE "^    error_page 404 /404.html;$" "$AVAILABLE"; then
    echo "    already done"
    nginx_backup=""
else
    nginx_backup="$HOME/nginx-hymods.de.bak-$STAMP"
    cp "$AVAILABLE" "$nginx_backup"
    python3 - "$AVAILABLE" "$ROOT" "$OLD_ROOT" <<'PY'
import pathlib, re, subprocess, sys

conf, root, old_root = pathlib.Path(sys.argv[1]), sys.argv[2], sys.argv[3]
text = conf.read_text()
# The HTTPS server sets its root at server level (4 spaces). The ACME location of the HTTP
# server has its own root, indented further; that one stays on the old folder for certbot.
server_root = re.compile(r"^    root (\S+);$", re.M)
roots = server_root.findall(text)
if roots != [root]:
    if roots != [old_root]:
        sys.exit(f"expected exactly one server-level root ({old_root}), found {roots}; edit by hand")
    text = server_root.sub(f"    root {root};", text)
if not re.search(r"^    error_page 404 /404\.html;$", text, re.M):
    index = re.compile(r"^    index index\.html;$", re.M)
    if len(index.findall(text)) != 1:
        sys.exit("expected exactly one 'index index.html;' in the HTTPS server; edit by hand")
    text = index.sub("    index index.html;\n    error_page 404 /404.html;", text)
subprocess.run(["sudo", "tee", str(conf)], input=text.encode(), check=True, stdout=subprocess.DEVNULL)
PY
    echo "    done; the file before: $nginx_backup"
fi
if [[ $replace_enabled -eq 1 ]]; then
    sudo ln -sfn "$AVAILABLE" "$ENABLED"
    echo "    $ENABLED -> $AVAILABLE"
fi
if ! sudo nginx -t; then
    if [[ -n "$nginx_backup" ]]; then
        sudo cp "$nginx_backup" "$AVAILABLE"
        echo "    nginx rejected the new configuration; the old one is back."
    fi
    fail "nginx -t failed, nothing reloaded."
fi
nginx_apply

step "6/7 Check"
for path in / /flexiisgoggles/ /flexiisgoggles/img/hero-ultragoggles.webp /assets/site.css /robots.txt; do
    code="$(local_get 443 -o /dev/null -w '%{http_code}' "https://hymods.de$path" || true)"
    echo "    https://hymods.de$path: HTTP $code"
    [[ "$code" == "200" ]] || fail "https://hymods.de$path answers HTTP $code."
done
local_get 443 "https://hymods.de/flexiisgoggles/" | grep -q "<title>FlexiisGoggles" \
    || fail "https://hymods.de/flexiisgoggles/ is not the new page."
code="$(local_get 443 -o /dev/null -w '%{http_code}' "https://hymods.de/no-such-page" || true)"
[[ "$code" == "404" ]] || fail "a missing page answers HTTP $code instead of 404."
local_get 443 "https://hymods.de/no-such-page" | grep -q "Not found" || fail "missing pages do not get /404.html."
echo "    missing pages: HTTP 404 with the site's 404 page"
# certbot's challenge still comes from the old folder: 404 (file missing), not the 301 to https
code="$(local_get 80 -o /dev/null -w '%{http_code}' "http://hymods.de/.well-known/acme-challenge/probe" || true)"
[[ "$code" == "404" ]] || fail "the ACME challenge path answers HTTP $code instead of 404."
echo "    ACME challenge path: still served from $OLD_ROOT"

step "7/7 ~/scripts/update-hymods.sh"
mkdir -p "$HOME/scripts"
install -m 755 "$APP/deploy/update-hymods.sh" "$HOME/scripts/update-hymods.sh"
echo "    installed"

echo
echo "Done. https://hymods.de now comes from $ROOT."
echo "Updates: push to GitHub, then run ~/scripts/update-hymods.sh on the server (no sudo)."
