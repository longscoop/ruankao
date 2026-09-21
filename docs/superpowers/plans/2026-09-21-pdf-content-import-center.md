# Phase 6 — PDF Content Import Center Plan

> Branch: `feat/soft-exam-v1`
>
> Goal: let operators import both exam-question PDFs and knowledge-lecture PDFs, preserve page visuals, review parser output, and publish lessons/questions without fake content or silent rewriting.

## Task 1 — Import + lesson persistence foundation
- Test first: Flyway schema, lesson/block ordering, import batch state transitions, admin-only API boundary.
- Add lesson / lesson_block / lesson_knowledge.
- Add content_import_batch / page / item / issue with source provenance and idempotent confirm key.
- Add USER / ADMIN role to app_user and protect `/api/v1/admin/**`.

## Task 2 — PDF parser engine
- Add Apache PDFBox.
- Extract text page-by-page and render source-page PNG snapshots.
- Classify QUESTION_BANK / LECTURE / MIXED.
- Detect title, TOC/sections, knowledge candidates, ordinary objective questions, embedded “典型真题”, answers/explanations.
- Flag composite questions, missing answers/options, parser ambiguity, and text/image mismatch candidates for manual review.
- Never invent or correct source text/answers.

## Task 3 — Dry run + confirm import workflow
- Multipart PDF upload -> original-object storage -> parser -> persisted preview.
- Store parsed items as reviewable JSON and page provenance.
- Confirm is idempotent.
- Approved lecture items create DRAFT/REVIEW course/chapter/lesson content.
- Approved question items create REVIEW questions; never publish automatically.
- Auto-create only DRAFT knowledge candidates, with provenance.

## Task 4 — Review / publish workflow
- Batch/item APIs: list, detail, approve, reject, edit structured payload, resolve issue.
- Publish approved lessons/questions explicitly.
- Require unresolved ERROR issues to be cleared before publish.
- Keep source page / source document references.

## Task 5 — Learner lesson delivery
- Add lesson summaries to course detail and `GET /api/v1/lessons/{id}`.
- Deliver ordered TEXT / IMAGE / TABLE / DIAGRAM / TIP / IMPORTANT / QUESTION / CASE blocks.
- Add miniapp lesson reader so imported knowledge lectures are actually consumable.

## Task 6 — Vue 3 admin import center
- Create `admin/` Vue 3 + TypeScript + Vite + Element Plus + Pinia app.
- PDF upload page, batch list, preview, source page image, issues, item approve/reject/edit, confirm/publish.
- Add Admin CI: tests + vue-tsc + production build.

## Task 7 — End-to-end verification
- Synthetic PDF integration test covers lecture sections + embedded question + page image preservation.
- Admin flow covers dry run -> review -> confirm -> publish -> learner lesson/question visible.
- Server CI, Miniapp CI, Admin CI all green before completion.
