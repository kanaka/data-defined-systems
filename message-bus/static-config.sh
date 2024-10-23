#!/bin/sh

export NATS_URL="nats://message-bus:4222"

echo "Sleep 3"
sleep 3
for target in "${@}"; do
  nats publish "dhcp" '{"action":"add","target":"'${target}'"}'
done
