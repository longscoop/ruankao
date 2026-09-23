# txy211 Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy the Soft Exam AI Java server and Vue admin application on txy211 using Docker Compose behind host Nginx, with persistent PostgreSQL and storage data.

**Architecture:** Host Nginx serves the compiled admin files on port 80, proxies `/api/` to a loopback-only Spring Boot container, and reads the upload storage bind mount at `/storage/`. Docker Compose provides a Java 17 server and private PostgreSQL 16; their persistent host bind mounts live under `/opt/ruankao/data`.

**Tech Stack:** Docker Compose v5, PostgreSQL 16, Java 17 JRE, Maven 3.9, Node.js 22, Nginx 1.24, Spring Boot 3, Vue 3/Vite.

**Spec:** `docs/superpowers/specs/2026-09-22-txy211-deployment-design.md`

## Global Constraints

- Deploy to `txy211` (`43.143.201.211`) at `http://43.143.201.211`; no domain name or TLS certificate is in scope.
- Keep host Nginx as the only public listener on port 80.
- Docker Compose runs only PostgreSQL 16 and the Java server; PostgreSQL has no published port and the server binds only `127.0.0.1:8080`.
- Persist PostgreSQL at `/opt/ruankao/data/postgres` and PDF/page-image storage at `/opt/ruankao/data/storage`.
- Keep database credentials and provider secrets in a mode-`0600` server-only `.env`; never commit them.
- Stop the existing `ruankao-postgres` container only after inspection; do not remove it until the replacement passes verification.
- Run server builds with Java 17 and admin builds with Node.js 22.
- Leave AI credentials unset; an AI request failure must not stop server startup or normal study/import behavior.

## Review Focus

- A missing deployment variable must make `docker compose config` fail before a container starts.
- PostgreSQL must not bind `5432` on the host.
- The server must bind only `127.0.0.1:8080`.
- Nginx must reject invalid syntax before the default site is replaced and must proxy the full `/api/` path.
- Database and upload files must remain in their host bind mounts after container recreation.

---

## File Structure

- `server/Dockerfile`: Java 17 Maven build and Java 17 runtime image.
- `deploy/txy211/docker-compose.yml`: PostgreSQL/server topology, persistent bind mounts, and environment mapping.
- `deploy/txy211/.env.example`: safe, complete variable names for the server-only environment file.
- `deploy/txy211/nginx.conf`: host Nginx static, API proxy, and storage configuration.
- `deploy/txy211/README.md`: copy, build, launch, verify, rollback, and update commands.
- `docs/superpowers/specs/2026-09-22-txy211-deployment-design.md`: final verification evidence.

### Task 1: Versioned deployment assets

**Files:**
- Create: `server/Dockerfile`
- Create: `deploy/txy211/docker-compose.yml`
- Create: `deploy/txy211/.env.example`
- Create: `deploy/txy211/nginx.conf`
- Create: `deploy/txy211/README.md`

**Interfaces:**
- Consumes: `server/pom.xml`, `server/src/main/resources/application.yml`, `admin/package.json`, `admin/.env.example`, and host paths `/opt/ruankao/data/{postgres,storage}`.
- Produces: a `ruankao-server` image; a Compose project with `postgres` and `server`; an Nginx site expecting admin files in `/var/www/ruankao-admin`.

- [ ] **Step 1: Verify deployment assets are absent (RED)**

Run:

```bash
test -f server/Dockerfile
test -f deploy/txy211/docker-compose.yml
test -f deploy/txy211/nginx.conf
```

Expected: all commands exit `1`, proving that the repository has no reproducible txy211 deployment definition.

- [ ] **Step 2: Add the Java 17 server image definition**

Create `server/Dockerfile`:

```dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 3: Add Compose, environment, Nginx, and operations documentation**

Create `deploy/txy211/docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - ${POSTGRES_DATA_DIR}:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 5s
      timeout: 5s
      retries: 20
  server:
    build:
      context: ../../server
    image: ruankao-server:latest
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "127.0.0.1:${SERVER_PORT}:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      STORAGE_LOCAL_ROOT: /data/ruankao-storage
      STORAGE_PUBLIC_BASE_URL: ${STORAGE_PUBLIC_BASE_URL}
      AI_DAILY_REQUEST_LIMIT: ${AI_DAILY_REQUEST_LIMIT}
    volumes:
      - ${STORAGE_DATA_DIR}:/data/ruankao-storage
```

Create `.env.example`:

```dotenv
POSTGRES_DB=ruankao
POSTGRES_USER=ruankao
POSTGRES_PASSWORD=replace-with-a-random-secret
POSTGRES_DATA_DIR=/opt/ruankao/data/postgres
STORAGE_DATA_DIR=/opt/ruankao/data/storage
STORAGE_PUBLIC_BASE_URL=http://43.143.201.211/storage
SERVER_PORT=8080
AI_DAILY_REQUEST_LIMIT=20
```

Create `nginx.conf` as a port-80 server block with root `/var/www/ruankao-admin`, SPA fallback `try_files $uri $uri/ /index.html`, `/api/` proxy to `http://127.0.0.1:8080`, and `/storage/` alias `/opt/ruankao/data/storage/` with `autoindex off` and `try_files $uri =404`. Include proxy headers `Host`, `X-Real-IP`, `X-Forwarded-For`, and `X-Forwarded-Proto`.

Write `README.md` with exact `rsync`, Node 22 `npm ci && npm run build` (the
admin already uses absolute `/api/v1` paths), Compose launch, `nginx -t`,
reload, verification, update, and rollback commands. Rollback restores the
host default Nginx site and starts the former `ruankao-postgres` container if
verification fails.

- [ ] **Step 4: Validate configuration (GREEN)**

Run:

```bash
cp deploy/txy211/.env.example deploy/txy211/.env
docker compose --env-file deploy/txy211/.env -f deploy/txy211/docker-compose.yml config
rg -n 'ports:|5432' deploy/txy211/docker-compose.yml
docker run --rm -v "$PWD/deploy/txy211/nginx.conf:/etc/nginx/conf.d/ruankao.conf:ro" nginx:1.24-alpine nginx -t
rm deploy/txy211/.env
```

Expected: Compose exits `0`; only the server has a loopback `8080` port mapping; PostgreSQL has no `ports`; Nginx syntax is successful.

- [ ] **Step 5: Build and verify admin production output**

Run:

```bash
export PATH="/Users/qilong/.nvm/versions/node/v22.23.2/bin:$PATH"
(cd admin && npm ci && VITE_API_BASE_URL=/api npm run check)
```

Expected: tests, typecheck, and Vite build exit `0`, and built output embeds `/api`.

- [ ] **Step 6: Review and commit deployment assets**

Run:

```bash
git diff --check
git diff -- server/Dockerfile deploy/txy211
git add server/Dockerfile deploy/txy211
git commit -m "ops: add txy211 deployment assets"
```

Expected: only versioned deployment assets are committed; no generated output or `.env` is tracked.

### Task 2: Deploy and verify txy211

**Files:**
- Modify: `docs/superpowers/specs/2026-09-22-txy211-deployment-design.md`

**Interfaces:**
- Consumes: Task 1 assets; target alias `txy211`; public IP `43.143.201.211`; Docker Compose; host Nginx; authorization to stop `ruankao-postgres`.
- Produces: running private PostgreSQL/server services, host Nginx serving admin and API, and recorded verification evidence.

- [ ] **Step 1: Inspect the replaceable database and prerequisites**

Run:

```bash
ssh txy211 'docker ps -a --filter name=^/ruankao-postgres$ --format "{{.ID}} {{.Image}} {{.Status}}"; docker compose version; sudo nginx -t'
```

Expected: existing container identity is recorded; Docker Compose and current Nginx validate.

- [ ] **Step 2: Copy source and build admin on txy211**

Run:

```bash
rsync -az --delete --exclude '.git' --exclude 'target' --exclude 'node_modules' --exclude 'dist' --exclude '.env' ./ txy211:/opt/ruankao/source/
ssh txy211 'set -eu; export PATH="/opt/node22/bin:$PATH"; cd /opt/ruankao/source/admin; npm ci; npm run build; sudo install -d -m 0755 /var/www/ruankao-admin; sudo rsync -a --delete dist/ /var/www/ruankao-admin/'
```

Expected: no local secret or generated build files transfer; `/var/www/ruankao-admin/index.html` exists. If `/opt/node22/bin` is unavailable, install/select Node 22 before building rather than using Node 24.

- [ ] **Step 3: Start the replacement stack with persistent data**

Run:

```bash
ssh txy211 'set -eu
sudo install -d -m 0755 /opt/ruankao/data/storage
sudo install -d -m 0700 /opt/ruankao/data/postgres
sudo chown -R 999:999 /opt/ruankao/data/postgres
sudo sh -c "umask 077; printf %s\\n \"POSTGRES_DB=ruankao\" \"POSTGRES_USER=ruankao\" \"POSTGRES_PASSWORD=$(openssl rand -hex 32)\" \"POSTGRES_DATA_DIR=/opt/ruankao/data/postgres\" \"STORAGE_DATA_DIR=/opt/ruankao/data/storage\" \"STORAGE_PUBLIC_BASE_URL=http://43.143.201.211/storage\" \"SERVER_PORT=8080\" \"AI_DAILY_REQUEST_LIMIT=20\" > /opt/ruankao/source/deploy/txy211/.env"
docker inspect ruankao-postgres >/opt/ruankao/previous-postgres-inspect.json
docker stop ruankao-postgres
cd /opt/ruankao/source/deploy/txy211
docker compose --env-file .env config
docker compose --env-file .env up --build -d'
```

Expected: directories and mode-`0600` environment file exist; former database is stopped but not removed; new private database/server start.

- [ ] **Step 4: Verify Flyway and loopback server before Nginx changes**

Run:

```bash
ssh txy211 'set -eu; cd /opt/ruankao/source/deploy/txy211; docker compose --env-file .env ps; docker compose --env-file .env logs --no-color server | tail -100; curl -sS -o /dev/null -w "%{http_code}\\n" http://127.0.0.1:8080/api/v1/exams'
```

Expected: PostgreSQL is healthy, server logs show Flyway and Spring Boot startup, direct unauthenticated API returns `401`.

- [ ] **Step 5: Activate host Nginx**

Run:

```bash
ssh txy211 'set -eu; sudo install -m 0644 /opt/ruankao/source/deploy/txy211/nginx.conf /etc/nginx/sites-available/ruankao; sudo ln -sfn /etc/nginx/sites-available/ruankao /etc/nginx/sites-enabled/ruankao; sudo rm -f /etc/nginx/sites-enabled/default; sudo nginx -t; sudo systemctl reload nginx'
```

Expected: Nginx reloads only after validation, and the default site is disabled.

- [ ] **Step 6: Verify public routes and persistence**

Run:

```bash
ssh txy211 'set -eu; curl -sS -o /dev/null -w "admin=%{http_code}\\n" http://127.0.0.1/; curl -sS -o /dev/null -w "api=%{http_code}\\n" http://127.0.0.1/api/v1/exams; curl -sS -o /dev/null -w "storage=%{http_code}\\n" http://127.0.0.1/storage/not-found; cd /opt/ruankao/source/deploy/txy211; docker compose --env-file .env exec -T postgres psql -U ruankao -d ruankao -Atc "select count(*) from flyway_schema_history"'
```

Expected: `admin=200`, `api=401`, `storage=404`, and Flyway history has one or more rows. Verify `http://43.143.201.211/` returns the same admin status externally.

- [ ] **Step 7: Record evidence and commit it separately**

Append actual Compose state, HTTP statuses, Flyway migration count, and the recoverable stopped state of the old database to the design document. Then run:

```bash
git diff --check
git add docs/superpowers/specs/2026-09-22-txy211-deployment-design.md
git commit -m "docs: record txy211 deployment verification"
```

- [ ] **Step 8: Final verification**

Run:

```bash
ssh txy211 'set -eu; cd /opt/ruankao/source/deploy/txy211; docker compose --env-file .env ps; curl -fsS http://127.0.0.1/ >/dev/null; status=$(curl -sS -o /dev/null -w "%{http_code}" http://127.0.0.1/api/v1/exams); test "$status" = 401'
git status --short
```

Expected: both services are running, admin is served, API authorization is enforced, and no secret or generated deployment artifact is in Git.
