# txy211 deployment

This deployment uses host Nginx, Docker Compose for the Java server and
PostgreSQL, and host bind mounts for persistent data.

The public origin is `https://www.e68q.cn`. Nginx uses the existing
Let's Encrypt certificate at `/etc/letsencrypt/live/www.e68q.cn/`. All HTTP
requests, including requests to the server IP, redirect to that HTTPS origin.
PDF imports accept files up to 20 MB; the Nginx API body limit and Spring
multipart request limit are 25 MB to allow for form overhead.

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
# The API modules already use absolute /api/v1 paths.  Leave the optional
# base URL empty for same-origin Nginx deployment.
npm run check
sudo install -d -m 0755 /var/www/ruankao-admin
sudo rsync -a --delete dist/ /var/www/ruankao-admin/
sudo chmod -R a+rX /var/www/ruankao-admin
```

Create persistent directories and a server-only environment file. The command
generates the database password on the target; never copy this file back to
the repository.

```bash
sudo install -d -m 0755 /opt/ruankao/data/storage
sudo install -d -m 0700 /opt/ruankao/data/postgres
sudo chown -R 999:999 /opt/ruankao/data/postgres
cd /opt/ruankao/source/deploy/txy211
read -r -p 'WeChat AppID: ' wechat_app_id
read -r -s -p 'WeChat AppSecret: ' wechat_app_secret
printf '\n'
umask 077
{
  printf '%s\n' 'POSTGRES_DB=ruankao' 'POSTGRES_USER=ruankao'
  printf 'POSTGRES_PASSWORD=%s\n' "$(openssl rand -hex 32)"
  printf '%s\n' 'POSTGRES_DATA_DIR=/opt/ruankao/data/postgres'
  printf '%s\n' 'STORAGE_DATA_DIR=/opt/ruankao/data/storage'
  printf '%s\n' 'STORAGE_PUBLIC_BASE_URL=https://www.e68q.cn/storage'
  printf 'WECHAT_APP_ID=%s\n' "$wechat_app_id"
  printf 'WECHAT_APP_SECRET=%s\n' "$wechat_app_secret"
  printf '%s\n' 'SERVER_PORT=8080' 'AI_DAILY_REQUEST_LIMIT=20'
  printf '%s\n' 'ADMIN_USERNAME=admin'
  printf 'ADMIN_PASSWORD=%s\n' "$(openssl rand -hex 24)"
} > .env
unset wechat_app_id wechat_app_secret
docker compose --env-file .env up --build -d
```

Install and activate host Nginx only after the Compose server has started.
Confirm the certificate files exist before replacing the active site:

```bash
sudo test -f /etc/letsencrypt/live/www.e68q.cn/fullchain.pem
sudo test -f /etc/letsencrypt/live/www.e68q.cn/privkey.pem
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
curl -sS -o /dev/null -w 'admin=%{http_code}\n' https://www.e68q.cn/
curl -sS -o /dev/null -w 'api=%{http_code}\n' https://www.e68q.cn/api/v1/exams
curl -sS -o /dev/null -w 'storage=%{http_code}\n' https://www.e68q.cn/storage/not-found
docker compose --env-file .env exec -T postgres psql -U ruankao -d ruankao -Atc 'select count(*) from flyway_schema_history'
```

Expected results are a running database/server, `admin=200`, `api=401`,
`storage=404`, and at least one Flyway history row.

## Update

Repeat the source copy and admin build. Keep the existing `.env`. Before
starting the updated Compose stack, set `STORAGE_PUBLIC_BASE_URL` to
`https://www.e68q.cn/storage` and add `WECHAT_APP_ID` and
`WECHAT_APP_SECRET` to that server-only file. Do not copy the example over it;
that would replace the existing database and administrator credentials.
Then run:

```bash
cd /opt/ruankao/source/deploy/txy211
docker compose --env-file .env up --build -d
sudo rsync -a --delete /opt/ruankao/source/admin/dist/ /var/www/ruankao-admin/
sudo chmod -R a+rX /var/www/ruankao-admin
sudo nginx -t && sudo systemctl reload nginx
```

## Miniapp release

In `miniapp/`, use Node.js 22 or newer and run `npm install` followed by
`npm run check`. The production build uses `https://www.e68q.cn` as its API
origin unless `VITE_API_BASE_URL` overrides it. Import
`miniapp/dist/build/mp-weixin` into WeChat Developer Tools with the real
miniapp AppID. Configure `https://www.e68q.cn` as the request domain in the
WeChat miniapp console, test on a phone, then upload the code for review and
release. Keep the AppSecret on the server only.

## Rollback before verification succeeds

```bash
cd /opt/ruankao/source/deploy/txy211
docker compose --env-file .env down
sudo rm -f /etc/nginx/sites-enabled/ruankao
sudo ln -sfn /etc/nginx/sites-available/default /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
docker start ruankao-postgres
```
