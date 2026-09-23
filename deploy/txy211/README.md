# txy211 deployment

This deployment uses host Nginx, Docker Compose for the Java server and
PostgreSQL, and host bind mounts for persistent data.

## Initial deployment

From the repository root, copy source without local output or credentials:

```bash
rsync -az --delete \
  --exclude '.git' --exclude '.superpowers' --exclude 'target' --exclude 'node_modules' --exclude 'dist' \
  --include '*/.env.example' --exclude '.env' --exclude '.env.*' \
  ./ txy211:/opt/ruankao/source/
```

On txy211, select Node.js 22, build the admin with its same-origin API base,
and install the static files:

```bash
export PATH="/opt/node22/bin:$PATH"
cd /opt/ruankao/source/admin
npm install --no-package-lock
VITE_API_BASE_URL=/api npm run check
sudo install -d -m 0755 /var/www/ruankao-admin
sudo rsync -a --delete dist/ /var/www/ruankao-admin/
```

Create persistent directories and a server-only environment file. The command
generates the database password on the target; never copy this file back to
the repository.

```bash
sudo install -d -m 0755 /opt/ruankao/data/storage
sudo install -d -m 0700 /opt/ruankao/data/postgres
sudo chown -R 999:999 /opt/ruankao/data/postgres
cd /opt/ruankao/source/deploy/txy211
umask 077
{
  printf '%s\n' 'POSTGRES_DB=ruankao' 'POSTGRES_USER=ruankao'
  printf 'POSTGRES_PASSWORD=%s\n' "$(openssl rand -hex 32)"
  printf '%s\n' 'POSTGRES_DATA_DIR=/opt/ruankao/data/postgres'
  printf '%s\n' 'STORAGE_DATA_DIR=/opt/ruankao/data/storage'
  printf '%s\n' 'STORAGE_PUBLIC_BASE_URL=http://43.143.201.211/storage'
  printf '%s\n' 'SERVER_PORT=8080' 'AI_DAILY_REQUEST_LIMIT=20'
} > .env
docker compose --env-file .env up --build -d
```

Install and activate host Nginx only after the Compose server has started:

```bash
sudo install -m 0644 /opt/ruankao/source/deploy/txy211/nginx.conf /etc/nginx/sites-available/ruankao
sudo ln -sfn /etc/nginx/sites-available/ruankao /etc/nginx/sites-enabled/ruankao
sudo nginx -t
sudo unlink /etc/nginx/sites-enabled/default
if ! sudo nginx -t; then
  sudo unlink /etc/nginx/sites-enabled/ruankao
  sudo ln -s /etc/nginx/sites-available/default /etc/nginx/sites-enabled/default
  sudo nginx -t
  exit 1
fi
sudo systemctl reload nginx
```

## Verify

```bash
cd /opt/ruankao/source/deploy/txy211
docker compose --env-file .env ps
curl -sS -o /dev/null -w 'admin=%{http_code}\n' http://127.0.0.1/
curl -sS -o /dev/null -w 'api=%{http_code}\n' http://127.0.0.1/api/v1/exams
docker compose --env-file .env exec -T postgres psql -U ruankao -d ruankao -Atc 'select count(*) from flyway_schema_history'
```

Expected results are a running database/server, `admin=200`, `api=401`, and
at least one Flyway history row. The public admin URL is
`http://43.143.201.211/`.

## Update

Repeat the source copy and admin build. Keep the existing `.env`, then run:

```bash
cd /opt/ruankao/source/deploy/txy211
docker compose --env-file .env up --build -d
sudo rsync -a --delete /opt/ruankao/source/admin/dist/ /var/www/ruankao-admin/
sudo nginx -t && sudo systemctl reload nginx
```

## Rollback before verification succeeds

```bash
cd /opt/ruankao/source/deploy/txy211
docker compose --env-file .env down
sudo rm -f /etc/nginx/sites-enabled/ruankao
sudo ln -sfn /etc/nginx/sites-available/default /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
docker start ruankao-postgres
```
