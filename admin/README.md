# Ruankao Admin

Vue 3 content operations UI for PDF question-bank / lecture imports.

## Run

```bash
cp .env.example .env
npm install
npm run dev
```

Set `VITE_API_BASE_URL` to the server base URL.

The current admin UI accepts an ADMIN user's Bearer session token. Promote an existing trusted `app_user` to `role='ADMIN'` in the deployment database before using the import endpoints. The server enforces `ROLE_ADMIN` for all `/api/v1/admin/**` routes.

## PDF workflow

1. Upload PDF + exam ID.
2. Review source-page image/text, parsed items and issues.
3. Edit/approve/reject items and resolve parser issues.
4. Confirm import to materialize lecture lessons in REVIEW.
5. Publish knowledge with explicit importance/frequency/minutes.
6. Publish questions with explicit source/difficulty/knowledge mapping.
7. Publish lessons with explicit knowledge mapping.

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
