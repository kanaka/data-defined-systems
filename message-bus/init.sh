#!/bin/sh

set -x

nats-server -js &

until nc -z localhost 4222; do
  echo "Waiting for NATS to start..."
  sleep 1
done

nats stream add events \
  --defaults \
  --subjects "dhcp" \
  --storage file \
  --retention limits \
  --max-msgs 1000 \
  --max-bytes 10MB \
  --ack \
  --max-age 1h

nats consumer add events balancer \
  --defaults \
  --filter "dhcp" \
  --ack=all \
  --deliver=subject \
  --target=balancer_events

# Client:
# nats consumer sub -r events balancer_events | while IFS= read message; do echo "message: ${message}"; done

sleep Infinity
