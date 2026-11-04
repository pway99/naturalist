# Run the full stack in Docker

This runs the packaged app with its own Postgres and image storage, shaped like production. It needs
only Docker, not Java or Maven. It doesn't touch the standing test database on port 5432.

## Start it

```bash
deploy/create-secrets.sh
```

```bash
docker compose up --build -d
```

The first build downloads every Maven dependency and takes several minutes.

| What | Where |
|---|---|
| The app | <http://localhost:8080> |
| Mail it sends, including verification links | <http://localhost:8025> |
| Postgres | `localhost:5433`, database and user `naturalist` |

Sign in to the admin pages as `admin`, with the password in `secrets/admin-credential`. Set
`NATURALIST_ADMIN_USERNAME` to use another name.

## What each file does

| File | Purpose |
|---|---|
| `Dockerfile` | Builds the `app` and `db-init` images from source |
| `compose.yaml` | The deployment itself: `db`, `db-init` and `app`, with no published ports |
| `compose.override.yaml` | Local only, loaded automatically: published ports, a mail catcher, and the challenge in log-only mode |
| `compose.production.yaml` | Production only: real mail, the real challenge, and `cloudflared` |
| `deploy/create-secrets.sh` | Generates the local secrets in `secrets/`, which git ignores |

## Database initialization

The `db-init` service runs `DeploymentDbInitializer` on every start, before the app. What it does
depends on the database:

| The database | Result |
|---|---|
| Is empty | Creates every schema with no `DROP` statements, loads the public catalog and its photos, and records completion in `deployment_initialization` |
| Is already initialized | Changes nothing and exits successfully |
| Has tables but no completion record | Refuses, changes nothing, and stops the app from starting |

It loads chemistry, library, plants, insects and the usage budgets. The plants and insects load
includes the seed field observations, because catalog photos hang off them. It loads no accounts,
naturalists, garden or soil records, so those tables start empty.

It doesn't apply later schema changes, because the project has no migration tool yet. A changed
schema needs a manual change to an existing database.

If initialization fails partway, the database has tables but no record, so later starts refuse. On a
database with nothing worth keeping, start again with the reset below.

## Stop and reset

Stop the stack and keep the data:

```bash
docker compose down
```

Stop the stack and **delete its database and photos**:

```bash
docker compose down -v
```

## Differences from production

- The bot challenge passes everything, because the real one needs the vendor's widget on the real
  hostname. Never use `compose.override.yaml` in production.
- Mail stops at Mailpit, and there is no Cloudflare edge or tunnel.
- The app has no metrics endpoint yet, so there is no Prometheus, Alertmanager or Grafana.
- Vision identification is off. To try it, set `NATURALIST_VISION_PROVIDER` and add the
  `model-credential` secret as `compose.production.yaml` does.

## Never publish the app's port in production

Tomcat trusts forwarded client addresses from private-range peers, and a published port reaches the
app from the Docker gateway, which is a private address. On a public host, a caller could then forge
their address and dodge the rate limits. That's why `compose.yaml` publishes nothing and the
override binds to `127.0.0.1` only.
