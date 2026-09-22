# Ruankao Admin

Vue 3 + TypeScript + Element Plus content operations console for PDF question-bank / lecture imports.

## Run

```bash
cp .env.example .env
npm install
npm run dev
```

Set `VITE_API_BASE_URL` to the server base URL.

## Administrator authentication

The admin console now uses a real username/password login flow instead of asking an operator to paste a Bearer Token manually.

Configure the server with environment variables:

```bash
ADMIN_USERNAME=admin
ADMIN_PASSWORD='replace-with-a-strong-password'
AUTH_SESSION_DAYS=30
```

Do not commit production credentials.

On the first successful administrator login, the server creates or binds an `ADMIN_PASSWORD` identity, ensures the linked `app_user.role` is `ADMIN`, and issues the same random database-backed Bearer session used by the rest of the authentication infrastructure. Admin passwords are read from server configuration and are not persisted to the database.

All `/api/v1/admin/**` routes are still enforced server-side with `ROLE_ADMIN`. The UI validates the saved session through `/api/v1/admin/auth/me` on startup and clears the local session automatically after a 401 response.

## PDF workflow

1. Sign in as an administrator.
2. Upload PDF + exam ID.
3. Review source-page image/text, parsed items and issues.
4. Edit/approve/reject items and resolve parser issues.
5. Confirm import to materialize lecture lessons in REVIEW.
6. Publish knowledge with explicit importance/frequency/minutes.
7. Publish questions with explicit source/difficulty/knowledge mapping.
8. Publish lessons with explicit knowledge mapping.

Nothing is automatically published by PDF parsing.

## Server configuration for PDF imports

For a simple single-node deployment, configure:

```bash
STORAGE_LOCAL_ROOT=/data/ruankao-storage
STORAGE_PUBLIC_BASE_URL=https://assets.example.com
```

`STORAGE_LOCAL_ROOT` is where uploaded source PDFs and rendered source-page PNGs are written. `STORAGE_PUBLIC_BASE_URL` must expose those objects for admin preview and learner lesson images.

For OSS/S3/COS or another object store, provide another `StorageProvider` implementation instead of using local filesystem storage.

AI structure-review suggestions use the existing OpenAI-compatible provider configuration:

```bash
AI_BASE_URL=...
AI_API_KEY=...
AI_MODEL=...
AI_PROVIDER_NAME=...
```

AI suggestions are advisory only. They are logged for usage accounting, do not mutate the stored import item, and cannot publish content.
