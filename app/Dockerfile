FROM ubuntu:24.04

RUN apt-get -y update
RUN apt-get -y install nodejs npm

RUN mkdir -p /app
ADD package.json /app/
RUN cd /app && npm install

RUN apt-get -y install iproute2 udhcpc

ADD index.js knexfile.js init.sh dhcp-event.sh /app/
CMD ["node", "/app/index.js", "eth0"]
