#!/bin/bash

export NATS_URL="nats://message-bus:4222"
mkdir -p /tmp/servers

cp /etc/haproxy-base.cfg /tmp/haproxy.cfg

haproxy -W -db -f /tmp/haproxy.cfg &
svr_pid=$!

# wait for stream to appear
while ! nats stream info events; do
  echo "Waiting for events stream to appear"
  sleep 1
done

while true; do
  nats consumer sub -r events balancer | \
  while IFS= read message; do
    echo "NATS message: ${message}"
    action=$(echo "${message}" | jq -r '.action')
    target=$(echo "${message}" | jq -r '.target')
    name=$(echo "${target}" | tr '.: ' '_')
    file=/tmp/servers/${name}.cfg
    case "${action}" in
      add)    echo "  server svr_${name} ${target} check" > ${file} ;;
      delete) rm -f ${file} ;;
    esac

    # Update the config and restart haproxy
    cat /etc/haproxy-base.cfg /tmp/servers/*.cfg > /tmp/haproxy.cfg
    kill -USR2 ${svr_pid}
  done
  echo "ERROR: consumer died, restarting"
  sleep 1
done
