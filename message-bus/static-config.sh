#!/bin/sh

export NATS_URL="nats://message-bus:4222"

for target in "${@}"; do
  nats publish "dhcp" '{"action":"add","target":"'${target}'"}'
done
