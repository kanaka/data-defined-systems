## Prerequisites

Host system requirements:
- docker and docker-compose
- node and npm

## Setup

Checkout this repo recursively:

```
git clone --recursive git@github.com/kanaka/conlink-demo
```

Install conlink npm dependencies:

```
for f in conlink dcmon dctest; do (cd $f && npm install); done
```

## Basic usage

Configure and start up the "app" mode:

```
bin/mdc app
docker compose up --force-recreate --build
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

## Scaling app service

Configure and start up "app" mode with 3 "app" replicas:

```
bin/mdc app
docker compose up --scale app=3 --force-recreate --build
```

Create a new user via the second app service:
```
curl localhost:8001/users/ -X POST -H "Content-type: application/json" -d '{"name":"Doug","email": "doug@example.com"}'
```

Then query the users via the third app service to show that the DB is
shared state and has been udpated with the new user:

```
curl localhost:8002/users/
```

