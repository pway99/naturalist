#!/bin/sh
# Generates the secrets the stack needs to start, in ./secrets (git-ignored). Existing files are
# left alone, because the database keeps the credential it was first created with.
#
# The directory is private to its owner and the files are world-readable on purpose. Compose
# bind-mounts each file, so the directory's mode protects it on the host, while the containers,
# which run as other users, can still read it. A private file would fail on a Linux host.
set -eu

cd "$(dirname "$0")/.."
mkdir -p secrets
chmod 700 secrets

for name in database-credential admin-credential; do
    if [ -s "secrets/$name" ]; then
        echo "secrets/$name already exists; leaving it alone"
    else
        openssl rand -hex 24 > "secrets/$name"
        echo "secrets/$name created"
    fi
    chmod 644 "secrets/$name"
done
