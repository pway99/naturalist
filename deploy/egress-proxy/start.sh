#!/bin/sh
# Renders the allowlist, then replaces this shell with Squid (ADR-029).
set -eu

# No mail host (the local stack, where mail goes straight to Mailpit): the mail rule matches
# nothing. .invalid is reserved, so it can never be a real host.
SMTP_HOST="${SMTP_HOST:-mail.invalid}"
SMTP_PORT="${SMTP_PORT:-1}"

sed -e "s|@SMTP_HOST@|$SMTP_HOST|" \
    -e "s|@SMTP_PORT@|$SMTP_PORT|" \
    /etc/egress-proxy/squid.conf.template > /tmp/squid.conf

# Squid writes its logs to files (see squid.conf.template); this puts them on the container's output.
# The directory too, so Squid can rename the files when it rotates them.
touch /var/log/squid/access.log /var/log/squid/cache.log
chown -R proxy:proxy /var/log/squid
tail -qF /var/log/squid/access.log /var/log/squid/cache.log &

# Keeps the files small: Squid renames each to .0 and reopens it. tail -F follows the new file.
(while sleep 60; do /usr/sbin/squid -f /tmp/squid.conf -k rotate 2>/dev/null || true; done) &

exec /usr/sbin/squid -f /tmp/squid.conf -NYC
