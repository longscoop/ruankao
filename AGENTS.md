# AGENTS.md

## Project
软考AI（Soft Exam AI）V1：面向“系统架构设计师”的微信小程序学习产品，以知识点为中心，串联视频、题库、AI讲解、错题复习和 Learning Engine 个性化学习计划。

## V1 Boundaries
- Client: uni-app + Vue 3 + TypeScript 微信小程序。
- Admin: Vue 3 + TypeScript + Vite + Element Plus。
- Server: Java 17 + Spring Boot 3 + Maven + PostgreSQL + Redis + Flyway。
- Architecture: modular monolith. Do not introduce microservices, MQ, registry/discovery, or distributed transactions in V1.
- First exam only: 系统架构设计师。
- AI is an enhancement layer. Learning Engine scoring/planning must be deterministic Java logic.
- AI-generated questions must never be labeled as real exam questions.
- V1 excludes community, live streaming, teacher marketplace, AI essay/case grading, native apps, and deep-learning knowledge tracing.

## Source of Truth
Read before implementing:
1. `docs/specs/soft-exam-v1.md`
2. `docs/implementation-plan.md`
3. The active task plan under `docs/superpowers/plans/`

If documents conflict: spec > implementation plan > task plan > code comments.

## Required Development Workflow
Every Task must follow:
1. Test first.
2. Run the test and confirm RED for the expected missing behavior.
3. Implement the minimum production code.
4. Run focused tests and then the affected suite; confirm GREEN.
5. Self-review the diff against the spec and task.
6. Commit the Task independently.

Do not combine multiple planned Tasks into one commit.

## Quality Rules
- No fake/mock business data presented as completed product functionality.
- No hard-coded exam/user/question records in UI business flows.
- Database schema changes use Flyway.
- API responses use DTOs; do not expose persistence entities directly.
- Business enums are centralized types, not scattered string literals.
- Payment, answer submission, mastery update, and daily-plan generation must be idempotent.
- Authorization is enforced server-side; hiding a button is never authorization.
- AI/provider/storage integrations must sit behind replaceable provider interfaces.
- CI tests must not require a real paid LLM API.
- Core learning data is persisted in PostgreSQL; Redis is never the sole store.
- A failed AI request must not break normal studying, video playback, or question answering flows.
- Prefer small cohesive classes/modules. Avoid speculative abstractions.

## Learning Engine Rules
- `mastery_score`: 0..100.
- No evidence => UI state “待评估”; do not invent a default displayed percentage.
- AI does not calculate or directly mutate mastery scores.
- Effective mastery applies retention decay at read/planning time; do not mutate stored mastery daily.
- One answer record may update mastery at most once.
- Daily plan generation must be idempotent per user/exam/date.
- Automatic same-day task expansion may not exceed 20% of configured daily target minutes.

## Testing
- Learning Engine unit-test target: >= 90% line coverage once the module is complete.
- New behavior must have a test that was observed failing before implementation.
- Use fake/in-memory providers for AI/storage/payment automated tests.
- Integration coverage must eventually include login -> exam profile -> assessment -> daily plan -> answer -> mastery -> wrong question -> review.

## Git / Commits
- Never implement feature work directly on `main`.
- Use feature branches.
- Commit messages should be scoped and imperative, e.g.:
  - `docs: add soft exam v1 implementation plan`
  - `test: define mastery scoring behavior`
  - `feat: implement mastery scoring`
- Do not force-push shared branches unless explicitly requested.

## Security
Never commit:
- WeChat secrets
- Payment keys
- Production database credentials
- AI API keys
- Access tokens

Use environment/config placeholders and documented variable names instead.
