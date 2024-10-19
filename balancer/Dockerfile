FROM haproxy:3.0.3

ADD monitor.sh /monitor.sh
ADD haproxy-base.cfg /etc

USER root

RUN apt-get -y update && \
    apt-get -y install curl jq iproute2 iputils-ping
RUN curl -sf https://binaries.nats.dev/nats-io/natscli/nats@v0.1.4 | sh && \
    mv nats /bin/

USER haproxy
