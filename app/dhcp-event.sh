#!/bin/bash

event=${1}; shift
log() { echo "dhcp-event: ${*}" >> /var/log/dhcp-event.log; }

case "${event}" in
  bound|renew) ;;
  *) log "ignoring DHCP event ${event}"; exit 0 ;;
esac

log "processing DHCP event ${event}. Settings:"
log $(env | grep "^[a-z]" | grep -v "()" | sed 's/^/    /')
read intf cmd < /var/run/dhcp.config

log "adding ip ${ip} to ${intf}"
ip addr add ${ip}/${mask} dev ${intf}
if [ "${gateway}" ]; then
  log "adding default route via ${gateway}"
  ip route add default via ${gateway} dev ${intf}
fi

log "finished processing DHCP event ${event}"

if [ "${cmd}" ]; then
  log "exec command: ${cmd}"
  exec ${cmd}
else
  log "sleeping forever"
  sleep Infinity
fi
