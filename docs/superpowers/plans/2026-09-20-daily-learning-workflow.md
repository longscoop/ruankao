# Daily Learning Workflow Implementation Plan

**Goal:** Connect persisted exam content and Learning Engine into the V1 learner workflow: exam profile, assessment, idempotent daily plans, answer orchestration, and authenticated-user-facing API boundaries.

**Spec:** `docs/specs/soft-exam-v1.md`

**Depends on:** completed Phase 1 and Phase 2.

**Tech Stack:** Java 17, Spring Boot 3.3.x, Spring Security, MyBatis-Plus, PostgreSQL, Flyway, Testcontainers, JUnit 5.

## Global Constraints

- User-specific APIs must never trust a client-supplied user id as authorization.
- Establish a minimal authenticated-principal boundary now; actual WeChat login credential exchange can be implemented later without changing service contracts.
- Server determines answer correctness from persisted standard answers; the client never submits a trusted `correct` flag.
- Daily plan generation is idempotent per `(user_id, exam_id, plan_date)`.
- Daily task allocation uses Phase 1 `DailyPlanAllocator`.
- Knowledge ranking uses stored mastery + retention + Phase 1 `PriorityScoreCalculator`.
- No AI is used to calculate mastery or daily-plan structure.
- Same-day dynamic task expansion is not implemented until there is an explicit task-completion workflow; do not fake it.
- No Redis requirement in Phase 3.
- Phase 3 may introduce REST controllers only for the learner workflow named in this plan.

---

### Task 1: Flyway V2 learner workflow schema

**Create:**
- `V2__create_daily_learning_workflow.sql`
- `DailyLearningSchemaMigrationTest.java`

Tables:
- `user_exam_profile`
- `assessment_session`
- `assessment_item`
- `study_plan`
- `study_task`

Critical constraints:
- profile: one row per user; target minutes only 15/30/60/90.
- assessment session UUID PK and status.
- assessment item unique session/question and unique session/sort_order.
- study plan unique user/exam/date.
- study task belongs to one plan and stores deterministic allocated minutes/task type/status.

RED: schema test sees missing tables/constraints.
GREEN: migration passes on real PostgreSQL.

---

### Task 2: User exam profile persistence

**Create:**
- `user/model/FoundationLevel.java`
- `user/persistence/UserExamProfileEntity.java`
- `user/persistence/UserExamProfileMapper.java`
- `user/UserExamProfileService.java`
- test first: `UserExamProfileServiceIntegrationTest.java`

Interface:
- `upsert(userId, examId, examDate, dailyTargetMinutes, foundationLevel)`
- `find(userId)`

Rules:
- user/exam ids positive.
- exam must exist.
- examDate cannot be in the past at write time.
- target minutes in 15/30/60/90.
- repeated upsert changes the same row, not creates another.

---

### Task 3: Standard answer support and assessment flow

**Modify / Create:**
- extend `QuestionService` with a creation overload that persists `standardAnswer`.
- `assessment/model/AssessmentStatus.java`
- `assessment/persistence/AssessmentSessionEntity.java`
- `assessment/persistence/AssessmentItemEntity.java`
- mappers and `AssessmentService.java`
- test first: `AssessmentServiceIntegrationTest.java`

Interface:
- `UUID start(userId, examId, int questionCount)`
- `List<QuestionEntity> listQuestions(sessionId)`
- `AnswerResult answer(sessionId, questionId, submittedAnswer, ...)`
- `AssessmentResult submit(sessionId)`

Rules:
- question selection is deterministic by question id among PUBLISHED questions in the exam.
- start fails if insufficient published questions.
- answer correctness is computed from persisted standard answer.
- each assessment item answered at most once.
- answer is persisted through `MasteryApplicationService.recordAnswer` with source ASSESSMENT.
- assessment answer updates mastery using the existing MasteryScoreCalculator and question knowledge weights.
- submit requires every assessment item answered.
- successful submit marks the user's profile assessment-completed.

---

### Task 4: Idempotent daily plan generation

**Create:**
- `learning/model/StudyPlanStatus.java`
- `learning/model/StudyTaskStatus.java`
- persistence entities/mappers for `study_plan` / `study_task`
- `DailyLearningService.java`
- test first: `DailyLearningServiceIntegrationTest.java`

Interface:
- `DailyPlanView getOrCreateToday(userId, Clock clock)`

Rules:
- load active user profile.
- compute days until exam.
- determine whether ACTIVE wrong questions exist.
- use `DailyPlanAllocator` for minutes.
- rank knowledge with effective mastery + `PriorityScoreCalculator`.
- choose top weak knowledge for WEAK_POINT allocation.
- choose not-yet-evidenced knowledge for NEW_KNOWLEDGE.
- persist REAL_EXAM allocation even when no specific knowledge id is selected.
- repeated request on the same date returns same plan id and does not duplicate tasks.
- estimated task minutes sum exactly to target minutes after unavailable-category redistribution.

---

### Task 5: Normal answer orchestration

**Create:**
- `question/QuestionAnswerService.java`
- test first: `QuestionAnswerServiceIntegrationTest.java`

Interface:
- submit a user answer for a persisted question/session source.

Rules:
- server computes correctness from standard answer.
- create immutable answer_record.
- calculate per-knowledge mastery deltas using current streak + question difficulty + confidence + knowledge weight.
- apply deltas idempotently.
- wrong answer -> `WrongQuestionService.recordWrong`.
- correct WRONG_REVIEW -> `recordCorrectReview` using updated effective mastery.
- repeated idempotency key cannot create a second answer record.

Ruling for idempotency key storage must be explicitly designed in schema before implementation; do not use process memory.

---

### Task 6: Authenticated principal boundary and learner REST API

**Dependencies:**
- Spring Security.

**Create:**
- `auth/RuankaoPrincipal.java`
- minimal security configuration requiring authentication for learner endpoints.
- controllers/DTOs for:
  - `PUT /api/v1/users/me/exam-profile`
  - `POST /api/v1/assessments`
  - `GET /api/v1/assessments/{id}/questions`
  - `POST /api/v1/assessments/{id}/answers`
  - `POST /api/v1/assessments/{id}/submit`
  - `GET /api/v1/learning/today`
- MockMvc tests first.

Rules:
- user id comes only from authenticated principal.
- unauthenticated requests return 401.
- DTOs only; no persistence Entity returned directly.
- WeChat login endpoint itself is not implemented in this task.

---

### Task 7: Phase 3 quality gate

Verify:
- `mvn -B verify`
- Phase 1 coverage gate remains green.
- PostgreSQL integration tests green.
- user id is never accepted from learner request body/query/path for `/users/me` and daily learning operations.
- no AI/Redis/payment dependency.
- no fake seed content.
- daily plan idempotency covered by integration test.
- assessment correctness evaluated server-side.

Completion:
- mark plan completed.
- link completed plan from `docs/implementation-plan.md`.
