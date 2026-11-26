#!/bin/sh
# Proves, from inside the running app container, that it reaches only the allowed hosts (ADR-029).
# Run it from the repository root after every deploy, with the same compose files as the deploy:
#
#   Production: deploy/egress-proxy/check.sh -f compose.yaml -f compose.production.yaml
#   Local:      deploy/egress-proxy/check.sh
#
# Locally the app also sits on the default network (compose.override.yaml), so the "no direct route
# out" and "outside names do not resolve" checks fail there by design. They are real in production.
set -eu
cd "$(dirname "$0")/../.."

# The mail check runs only where mail goes through the proxy (production). Locally mail goes
# straight to Mailpit.
mail_host="$(docker compose "$@" exec -T app printenv SPRING_MAIL_PROPERTIES_MAIL_SMTP_PROXY_HOST >/dev/null 2>&1 \
    && docker compose "$@" exec -T app printenv SPRING_MAIL_HOST || true)"
mail_port="$(docker compose "$@" exec -T app printenv SPRING_MAIL_PORT 2>/dev/null || true)"

if [ -n "$mail_host" ]; then
    docker compose "$@" exec -T app bash -s "$mail_host" "$mail_port" < deploy/egress-proxy/probe.sh
else
    docker compose "$@" exec -T app bash -s < deploy/egress-proxy/probe.sh
fi
