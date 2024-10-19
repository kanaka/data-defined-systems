FROM nats:2.10.20-alpine

RUN apk update && \
    apk add curl
RUN curl -sf https://binaries.nats.dev/nats-io/natscli/nats@v0.1.4 | sh && \
    mv nats /bin/nats

ADD init.sh /init.sh
ADD static-config.sh /static-config.sh
