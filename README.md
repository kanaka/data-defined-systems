## Prerequisites

Checkout this repo recursively:

```
git clone --recursive git@github.com/kanaka/conlink-demo
```

Install conlink npm dependencies:

```
(cd conlink && npm install)
```

## Basic usage

Configure and start up basic mode:

```
bin/mdc basic
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

Shutdown and fully remove all containers and state:

```
docker compose down --remove-orphans --volumes
```
