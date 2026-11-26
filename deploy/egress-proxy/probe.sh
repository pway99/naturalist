#!/usr/bin/env bash
# Proves the egress allowlist from inside a container on the internal network (ADR-029). Bash only:
# the app image has no curl or wget. Run it through check.sh (inside the running app), or pipe it
# into any container on the internal network:  bash -s [mail-host mail-port] < probe.sh
#
# Exits non-zero if any answer is not the expected one.
set -u

PROXY_HOST="${PROXY_HOST:-egress-proxy}"
PROXY_PORT="${PROXY_PORT:-3128}"
failures=0

# Sends one request line to the proxy and prints the status code it answers (000: no answer).
ask() {
    local code=000
    if exec 3<>"/dev/tcp/$PROXY_HOST/$PROXY_PORT"; then
        printf '%s HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n' "$1" "$2" >&3
        read -r -t 20 _ code _ <&3 || code=000
        exec 3<&- 3>&-
    fi
    echo "${code:-000}"
}

expect() {
    local want="$1" got="$2" what="$3"
    if [ "$got" = "$want" ]; then
        echo "ok    $what ($got)"
    else
        echo "FAIL  $what: expected $want, got $got"
        failures=$((failures + 1))
    fi
}

expect 200 "$(ask 'CONNECT api.anthropic.com:443' api.anthropic.com:443)" "model API is reachable"
expect 200 "$(ask 'CONNECT challenges.cloudflare.com:443' challenges.cloudflare.com:443)" "challenge endpoint is reachable"
expect 403 "$(ask 'CONNECT example.com:443' example.com:443)" "an unlisted host is refused"
expect 403 "$(ask 'CONNECT api.anthropic.com:80' api.anthropic.com:80)" "a listed host on another port is refused"
expect 403 "$(ask 'CONNECT 160.79.104.10:443' 160.79.104.10:443)" "an IP literal is refused"
expect 403 "$(ask 'CONNECT 169.254.169.254:80' 169.254.169.254:80)" "the metadata address is refused"
expect 403 "$(ask 'GET http://api.anthropic.com/' api.anthropic.com)" "plain HTTP is refused"

if timeout 5 bash -c 'exec 3<>/dev/tcp/1.1.1.1/443' 2>/dev/null; then
    echo "FAIL  a direct connection out succeeded"
    failures=$((failures + 1))
else
    echo "ok    no direct route out"
fi

if getent hosts example.com >/dev/null 2>&1; then
    echo "FAIL  an outside name resolved: DNS leaks (needs Docker Engine 26 or later)"
    failures=$((failures + 1))
else
    echo "ok    outside names do not resolve"
fi

if [ $# -ge 2 ]; then
    code="$(ask "CONNECT $1:$2" "$1:$2")"
    case "$code" in
        200) echo "ok    mail host is reachable ($code)" ;;
        403) echo "FAIL  mail host: the proxy refused it ($code); check SMTP_HOST/SMTP_PORT on egress-proxy"
             failures=$((failures + 1)) ;;
        *)   echo "FAIL  mail host: the proxy could not reach it ($code); the network or provider blocks the port"
             failures=$((failures + 1)) ;;
    esac
fi

[ "$failures" -eq 0 ] && echo "egress allowlist holds" || echo "$failures check(s) failed"
exit "$failures"
