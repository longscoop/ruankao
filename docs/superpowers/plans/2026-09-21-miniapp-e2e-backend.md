# Miniapp End-to-End Backend Completion Plan

> Branch: `feat/soft-exam-v1`
>
> Goal: make every implemented V1 miniapp page usable against real server APIs without fake business data.

## Task 1 — WeChat authentication and exam catalog
- Test first: WeChat code exchange, stable user identity, bearer authentication, invalid token rejection, exam list.
- Add `app_user`, `auth_identity`, `auth_session` persistence.
- Add replaceable `WechatIdentityProvider` and HTTP implementation using `WECHAT_APP_ID` / `WECHAT_APP_SECRET`.
- Store only SHA-256 token hashes server-side.
- Add `POST /api/v1/auth/wechat/login`, bearer filter, and `GET /api/v1/exams`.

## Task 2 — Course / video / knowledge learner REST
- Test published-content filtering, user progress, transcript, mastery pending semantics, access URL generation.
- Add `GET /courses`, `GET /courses/{id}`, `GET /videos/{id}`, `PUT /videos/{id}/progress`, `GET /knowledge/{id}`, `GET /knowledge/tree`.
- Generate video play URLs through `StorageProvider`.

## Task 3 — Practice sessions and wrong questions
- Persist generic question sessions and selected question IDs.
- Test source filtering, ownership, answer idempotency, option exposure, explanation feedback, wrong-question history.
- Add `POST /question-sessions`, questions, answers, and `GET /users/me/wrong-questions`.

## Task 4 — AI provider layer
- Add replaceable `AiProvider`, fake test provider, OpenAI-compatible HTTP provider configurable for Qwen / DeepSeek / Doubao-compatible endpoints.
- Persist AI usage logs.
- Add chat and question-explain endpoints with quota/error isolation.
- AI never changes mastery or canonical answers.

## Task 5 — Weekly analytics
- Test server aggregation from persisted study/video/answer/mastery data.
- Add `GET /users/me/stats/weekly` and weak-knowledge output.
- Preserve “待评估” through evidence_count rather than invented mastery.

## Task 6 — Miniapp integration and release
- Remove capability degradation flags once corresponding APIs exist.
- Align DTOs/options/IDs/idempotency headers with the real server.
- Add login -> profile -> assessment -> plan -> course/video -> practice/wrong -> AI -> weekly report integration coverage.
- Require server CI and Miniapp CI green before marking this plan complete.
