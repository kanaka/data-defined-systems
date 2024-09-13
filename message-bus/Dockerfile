FROM nats:2.10-alpine

RUN apk update && \
    apk add curl && \
    curl -sf https://binaries.nats.dev/nats-io/natscli/nats@latest | sh && \
    mv nats /bin/nats

ADD init.sh /init.sh
ADD static-config.sh /static-config.sh
