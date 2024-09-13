FROM node:22 as build

RUN apt-get -y update && \
    apt-get -y install libpcap-dev

#RUN apt-get -y install defaulat-jdk-headless && \
#    npm install -g shadow-cljs

# Separate npm and clojure deps from main app build
RUN mkdir -p /app
ADD shadow-cljs.edn package.json /app/
RUN cd /app && npm --unsafe-perm install
#RUN cd /app && shadow-cljs info

# main app build
ADD src/ /app/src/
ADD clj-protocol/ /app/clj-protocol/
ADD dhcp-config.edn /app/
#RUN cd /app && \
#    shadow-cljs compile dhcp-server && \
#    chmod +x build/*.js

FROM node:22-slim as run

RUN apt-get -y update
RUN apt-get -y install libpcap-dev tcpdump iputils-ping curl iproute2

ENTRYPOINT []

COPY --from=build /app/ /app/
