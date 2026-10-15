#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .tls
openssl req -x509 -newkey rsa:2048 -nodes -days 7 -subj '/CN=localhost' \
  -addext 'subjectAltName=DNS:localhost,DNS:host.docker.internal,IP:127.0.0.1' \
  -keyout .tls/localhost.key -out .tls/localhost.crt
