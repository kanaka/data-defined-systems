#!/bin/bash

intf="${1}"; shift
die() { echo >&2 "${*}"; exit 1; }

wait_for_if() {
  local if=$1
  while true; do
    ip addr show $if >/dev/null 2>&1 && return 0
    sleep 1
  done
}

[ "${intf}" -a "${*}" ] || die "Usage: ${0} intf cmd args..."

echo "Waiting for interface ${intf} to appear"
wait_for_if ${intf}

echo "${intf} ${*}" > /var/run/dhcp.config

touch /var/log/dhcp-event.log
tail -f /var/log/dhcp-event.log &

echo "Starting udhcpc client"
ip link set dev ${intf} up
udhcpc -b -i ${intf} -s /app/dhcp-event.sh

sleep Infinity
