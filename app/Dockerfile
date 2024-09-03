FROM ubuntu:24.04

RUN apt-get -y update
RUN apt-get -y install nodejs npm

RUN mkdir -p /app
ADD package.json /app/
RUN cd /app && npm install

ADD index.js knexfile.js /app/
CMD ["node", "/app/index.js"]
