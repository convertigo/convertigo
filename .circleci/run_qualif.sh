#!/usr/bin/env bash
set +x
set -eu

exec python3 .circleci/qualif_runner.py "$@"
