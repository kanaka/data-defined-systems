## Prerequisites

Host system requirements:
- docker and docker-compose
- node and npm

## Setup

Checkout this repo recursively:

```
git clone --recursive git@github.com:kanaka/data-defined-systems
```

Install conlink npm dependencies:

```
for f in conlink dcmon dctest; do (cd $f && npm install); done
```

## Basic usage

Configure and start up the "dhcp" mode:

```
bin/mdc dhcp
docker compose up --force-recreate --build
```

Monitor the startup status:

```
dcmon
```

Curl current users:

```
curl localhost:8000/users | jq '.'
```

Create a new user:

```
curl localhost:8000/users/ -X POST -H "Content-type: application/json" -d '{"name":"Carol","email": "carol@example.com"}'
```

Update an existing user (the version value must match the current
version of the user record):

```
curl localhost:8000/users/1 -X PUT -H "Content-type: application/json" -d '{"email": "alice@nowhere.net", "version":0}'
```

Shutdown and fully remove all containers and volumes:

```
docker compose down --remove-orphans --volumes -t1
```

## Scaling api service

Configure and start up "dhcp" mode and then scale it to 3 "api" replicas:

```
bin/mdc dhcp
docker compose up --scale api=3 --force-recreate --build
```

