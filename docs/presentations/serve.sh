#!/usr/bin/env bash

PORT=${1:-${PORT:-8000}}
SRC=source/
DST=./$(git rev-parse --abbrev-ref HEAD)
REVEAL=./reveal.js-submodule

die() { echo "${*}"; exit 1; }

which fswatch >/dev/null 2>/dev/null || die "Could not find fswatch"

[ -f  ${REVEAL}/.git ] || die "Need to submodule init/update to pull in reveal.js"

# Kill child processes on exit
trap "trap - SIGTERM && kill -- -$$" SIGINT SIGTERM EXIT

# Update reveal.js content in web serving directory
#rsync -a --out-format="%n%L" ${REVEAL}/ reveal.js/ --exclude index.html
rsync -a --out-format="%n%L" ${REVEAL}/ reveal.js/ --exclude .git

rsync -a --out-format="%n%L" ${SRC}/ ${DST}/
(
  fswatch -r -x --exclude ".*~" ${SRC} |
  while read path events; do \
    #echo ">> path: ${path}, events: ${events}"
    rsync -a --out-format="%n%L" ${SRC}/ ${DST}/
  done
) &

cd ${DST}/..
python3 -m http.server ${PORT} &
sleep 0.25

echo "Watching files in '${SRC}'. Serving '${DST}/..' on port ${PORT}."
echo "Press Ctrl-C to stop."
sleep 86400

