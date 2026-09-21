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
