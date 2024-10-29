# Data-Defined Tools Developing and Testing Multi-Service Networked Systems

This repository contains a full demo system and documentation
describing an approach and a set of tools for developing and testing
complex multi-service networked systems. There are two primary themes
of the approach which are embodied in the tools: data-defined and
containerized.

The tools include:

* docker compose: data-defined container services
* [conlink](https://lonocloud.github.io/conlink/): data-defined container networking
* [mdc](https://github.com/lonocloud/conlink/blob/master/mdc): data-defined service group (module) composition
* [resolve-deps](https://github.com/viasat/resolve-deps): data-defined module dependencies
* [dcmon](https://github.com/lonocloud/dcmon): data-defined system status
* [clj-protocol](https://github.com/lonocloud/clj-protocol): data-defined protocols
* [instacheck](https://github.com/kanaka/instacheck): data-defined generative tests
* [dctest](https://viasat.github.io/dctest): data-defined test-suites

In addition all the tools (apart from docker compose) are implemented
primarily in Clojure. However, Clojure knowledge is not required to
make use of these tools.

[Here](https://kanaka.github.io/data-defined-systems/presentations/main)
is a presentation about the approach and tools. The presentation uses
reveal.js (press [space] to progress). Most of the text content is in
the speaker notes (press [s] to bring up the speaker mode with notes.

## Prerequisites

Host system requirements:
- docker and docker-compose
- node and npm

Checkout this repo recursively:

```
git clone --recursive git@github.com:kanaka/data-defined-systems
cd data-defined-systems
```

Install conlink npm dependencies:

```
for f in conlink dcmon dctest; do (cd $f && npm install); done
```

Add `bin/` directory to the PATH either directly or via direnv:

```
export PATH=$(pwd)/bin:$PATH

# OR

eval "$(direnv hook bash)"
direnv allow .
```

## Basic usage

Configure and start up the "dhcp" mode:

```
mdc dhcp
docker compose up --force-recreate --build
```

Monitor the system startup status:

```
dcmon
```

Query the current users (using simple wrapper that adds JSON header
and formats JSON output):

```
CURL localhost:8000/users
```

Create a new user:

```
CURL localhost:8000/users/ -X POST -d '{"name":"Carol","email": "carol@example.com"}'
```

Update an existing user:

```
CURL localhost:8000/users/1 -X PUT -d '{"email": "alice@nowhere.net"}'
```

Shutdown and fully remove all containers and volumes:

```
docker compose down --remove-orphans --volumes -t1
```

## Scale api service

Configure and start up "dhcp" mode and then scale it to 3 "api" replicas:

```
mdc dhcp
docker compose up --scale api=3 --force-recreate --build
```

