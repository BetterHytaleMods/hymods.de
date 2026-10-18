#!/usr/bin/env bash
set -euo pipefail

# Brings hymods.de up to date. The site is plain files that nginx serves straight from the
# clone, so a fast-forward pull is all it takes: no build, no reload, no sudo.
#
# setup-server.sh installs this as ~/scripts/update-hymods.sh:
#
#     ssh <host> '~/scripts/update-hymods.sh'
#
# Pushed to GitHub first? Only what is on GitHub arrives here.

APP_DIR="/opt/hymods.de/app"

[[ $EUID -eq 0 ]] && { echo "Do not run this with sudo." >&2; exit 1; }
[[ -d "$APP_DIR/.git" ]] || { echo "$APP_DIR is not a clone; run deploy/setup-server.sh first." >&2; exit 1; }

before="$(git -C "$APP_DIR" rev-parse --short HEAD)"
git -C "$APP_DIR" pull --ff-only --quiet
after="$(git -C "$APP_DIR" rev-parse --short HEAD)"

if [[ "$before" == "$after" ]]; then
    echo "hymods.de is up to date ($after)."
else
    echo "hymods.de updated: $before -> $after"
    git -C "$APP_DIR" --no-pager log --oneline "$before..$after"
fi
