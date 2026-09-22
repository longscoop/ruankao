# txy211 Single-Host Deployment Design

## Purpose

Deploy the Soft Exam AI server and admin application on `txy211`
(`43.143.201.211`) for initial HTTP access at `http://43.143.201.211`.
There is no domain name or TLS certificate in scope for this deployment.

## Confirmed topology

- Host Nginx remains the public web server on port 80.
- Docker Compose runs the Spring Boot server and PostgreSQL 16.
- The existing `ruankao-postgres` container may be stopped and removed after
  its state has been inspected; the deployed database is a new, isolated
  PostgreSQL instance.
- PostgreSQL data is persisted in a named Docker volume. Imported PDF files
  and rendered page images are persisted in a separate named Docker volume.

```text
browser
  -> host Nginx :80
       /            -> static admin build
       /api/         -> Docker Compose server :8080 (loopback only)
       /storage/     -> read-only storage Docker volume

Docker Compose
  server (Java 17) -> postgres:16 (private Docker network)
```

## Components

### Docker Compose

The Compose project has two services:

- `postgres`: PostgreSQL 16 with database and application credentials supplied
  through a server-only environment file. It exposes no host port and owns a
  named `postgres-data` volume.
- `server`: built with Maven and run on a Java 17 JRE. It connects to
  `postgres` through the Compose network, runs Flyway migrations during
  startup, binds `127.0.0.1:8080`, and mounts `ruankao-storage` at the
  configured local storage root.

The application image is built from the repository source on the target host.
No application, PostgreSQL, AI, WeChat, or storage secret is committed to Git.

### Host Nginx

Nginx serves the admin production build from `/var/www/ruankao-admin`.
The production admin build has `VITE_API_BASE_URL=/api`, so API calls are
same-origin. Nginx proxies `/api/` to the loopback-bound server and serves
`/storage/` read-only from the Docker volume mount. The default Nginx site is
disabled only after the new configuration passes `nginx -t`.

## Data and security

- Compose does not publish PostgreSQL to the host network.
- The generated server-only `.env` file is mode `0600` and contains a random
  database password.
- API and storage traffic are HTTP because the user has no domain/certificate;
  TLS is an explicit future follow-up.
- AI configuration remains unset; AI requests can fail independently without
  stopping normal study or import behavior.

## Deployment sequence and rollback

1. Verify the source build and admin production build.
2. Copy the checked-out source and deployment assets to `/opt/ruankao` without
   local dependency/build directories.
3. Inspect and stop the existing `ruankao-postgres` container, as authorized.
4. Generate the server-only environment file, start Compose, and wait for the
   database and server.
5. Install and validate the host Nginx configuration, then reload Nginx.
6. Verify Flyway startup, direct loopback API availability, Nginx `/api/`
   proxying, and admin HTML delivery.

If the application start or proxy verification fails, stop the new Compose
project and restore Nginx's prior default configuration. The prior PostgreSQL
container will not be deleted until the new stack is verified.

## Acceptance criteria

- `docker compose ps` shows a healthy PostgreSQL service and running server.
- Server logs show successful Flyway migration and application startup.
- `http://127.0.0.1:8080` is reachable only on the host.
- `http://43.143.201.211/api/...` reaches the server through Nginx.
- `http://43.143.201.211/` returns the admin application.
- PostgreSQL and upload data use persistent Docker volumes.

## Completion Evidence

- Target: `txy211` (`43.143.201.211`) with host Nginx serving the admin
  application on port 80.
- Docker Compose services: PostgreSQL 16 was healthy and the Java 17 server
  was running with only `127.0.0.1:8080` published.
- Flyway: 9 migrations were applied successfully to the new `ruankao`
  PostgreSQL database.
- Route checks: local and public admin requests returned `200`; unauthenticated
  local and public `/api/v1/exams` requests returned `401`; a missing storage
  object returned `404`.
- Persistence: PostgreSQL and storage use `/opt/ruankao/data/postgres` and
  `/opt/ruankao/data/storage` host bind mounts.
- Previous state: the former `ruankao-postgres` container remains present but
  stopped, so it can be recovered without recreating it.
