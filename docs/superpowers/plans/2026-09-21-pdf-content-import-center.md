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
- Confirm materializes approved lecture items as REVIEW lessons. Question/knowledge candidates remain review items until the operator explicitly supplies the required publication metadata.
- Questions and knowledge points are never auto-published from parser output.

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


## Completion

Status: **PDF Content Import Center completed on 2026-09-21**.

Implemented:
- PDFBox page text extraction plus source-page PNG rendering.
- QUESTION_BANK / LECTURE / MIXED / UNKNOWN classification.
- Objective-question parsing with options, source answer and source explanation preservation.
- Cross-page question state: a question can start on one PDF page and receive its answer/explanation on a later page.
- Composite / ambiguous questions, missing answers and missing options are surfaced as review issues; parser output never invents missing source content.
- Covers and table-of-contents pages are retained as source evidence but excluded from learner lesson bodies.
- Lesson / lesson_block / lesson_knowledge persistence and question source provenance.
- Admin-only upload, dry run, page preview, item edit/approve/reject, issue resolution, idempotent confirm and explicit publish.
- AI-assisted structure review is suggestion-only and cannot mutate source JSON or publish content.
- Local filesystem StorageProvider uploads for development; production can replace StorageProvider with object storage.
- Vue 3 admin import center and learner miniapp lesson reader.
- Mixed-PDF E2E verifies import -> review -> explicit knowledge/question/lesson publication -> learner course/lesson/practice visibility.

Publication safety:
- Knowledge point importance, exam frequency and estimated study minutes must be supplied explicitly.
- Question difficulty, source and knowledge weights must be supplied explicitly.
- Open ERROR issues block publication.
- Parsing never changes a PDF answer/analysis silently and never infers that a question is a real exam question.
- Imported lecture lessons are REVIEW until explicitly published.

Final verification:
- Server CI: **126/126 tests passed**, Maven verify / JaCoCo checks passed.
- Admin CI: **3/3 tests passed**, vue-tsc passed, Vite production build passed.
- Miniapp CI after learner lesson delivery: **21/21 tests passed**, vue-tsc passed, mp-weixin production build passed.
