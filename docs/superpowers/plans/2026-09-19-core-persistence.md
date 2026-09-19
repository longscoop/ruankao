# Exam / Knowledge / Question Persistence Implementation Plan

> **Execution mode:** inline, task-by-task. Every behavior follows RED -> GREEN -> full suite -> self-review -> independent commit.

**Goal:** Persist the V1 exam content and learning evidence model in PostgreSQL, with Flyway-managed schema, MyBatis-Plus repositories, validated question-to-knowledge weights, idempotent mastery application, and persistent wrong-question lifecycle.

**Spec:** `docs/specs/soft-exam-v1.md`

**Depends on:** completed Phase 1 Learning Engine.

**Tech Stack:** Java 17, Spring Boot 3.3.x, MyBatis-Plus 3.5.17, PostgreSQL, Flyway, Testcontainers 2.0.5, JUnit 5.

## Global Constraints

- PostgreSQL is the source of truth. Redis is not introduced in Phase 2.
- Flyway owns all schema creation/change.
- Integration tests use a real PostgreSQL Testcontainer, not H2.
- Persistence entities are internal; later APIs must use DTOs.
- No hard-coded product data or seeded fake exam/question records.
- `question_knowledge.weight` is in (0, 1], and all links for one question must sum to 1.0.
- Exactly one primary knowledge link is required per question.
- Answer history is append-only business evidence.
- A single answer record may apply mastery at most once.
- Mastery application must be transactional: claiming the answer and updating all related mastery rows either commit together or rollback together.
- Wrong-question history remains queryable after MASTERED.
- Phase 2 does not expose HTTP APIs, authentication, daily planning APIs, Redis, AI, video, or payment.

## Planned schema

Tables:
- `exam`
- `knowledge_point`
- `question`
- `question_knowledge`
- `answer_record`
- `user_knowledge_mastery`
- `wrong_question`

User ids are stored as `BIGINT` without a foreign key until the user/auth schema is introduced.

---

### Task 1: PostgreSQL persistence foundation and Flyway V1 schema

**Files:**
- Modify: `server/pom.xml`
- Create: `server/src/main/resources/application.yml`
- Create: `server/src/main/resources/db/migration/V1__create_core_learning_schema.sql`
- Create: `server/src/test/java/com/longscoop/ruankao/support/PostgresIntegrationTest.java`
- Test first: `server/src/test/java/com/longscoop/ruankao/persistence/PersistenceSchemaMigrationTest.java`
- Modify test infrastructure if required: `RuankaoApplicationTests.java`

**RED:** migration test fails before datasource/Flyway/schema exists.

**GREEN verifies:**
- Flyway migration succeeds against PostgreSQL.
- all seven Phase 2 tables exist.
- critical uniqueness/check constraints exist and reject invalid rows.
- application context can start in integration tests with PostgreSQL.

---

### Task 2: Exam persistence service

**Files:**
- Test first: `exam/ExamServiceIntegrationTest.java`
- Create:
  - `exam/model/ExamStatus.java`
  - `exam/persistence/ExamEntity.java`
  - `exam/persistence/ExamMapper.java`
  - `exam/ExamService.java`

**Interface:**
- `long create(String code, String name, ExamStatus status)`
- `Optional<ExamEntity> findById(long id)`
- `List<ExamEntity> listActive()`

**Rules:**
- code and name trimmed and nonblank.
- exam code unique.
- `listActive` returns ACTIVE only, sorted by id.

---

### Task 3: Knowledge point persistence and tree ordering

**Files:**
- Test first: `knowledge/KnowledgePointServiceIntegrationTest.java`
- Create:
  - `knowledge/model/KnowledgeStatus.java`
  - `knowledge/persistence/KnowledgePointEntity.java`
  - `knowledge/persistence/KnowledgePointMapper.java`
  - `knowledge/KnowledgePointService.java`

**Interface:**
- `long create(...)`
- `List<KnowledgePointEntity> listForExam(long examId)`

**Rules:**
- `importance` 1..5.
- `examFrequency` 0..100.
- `estimatedMinutes > 0`.
- root has null parent and level >= 1.
- child parent must exist in same exam.
- code unique within one exam.
- list order: level, sortOrder, id.

---

### Task 4: Question + QuestionKnowledge persistence with weight validation

**Files:**
- Test first: `question/QuestionServiceIntegrationTest.java`
- Create:
  - `question/model/QuestionSource.java`
  - `question/model/QuestionType.java`
  - `question/model/QuestionStatus.java`
  - `question/persistence/QuestionEntity.java`
  - `question/persistence/QuestionKnowledgeEntity.java`
  - `question/persistence/QuestionMapper.java`
  - `question/persistence/QuestionKnowledgeMapper.java`
  - `question/QuestionKnowledgeLink.java`
  - `question/QuestionService.java`

**Interface:**
- create a question and links in one transaction.
- retrieve question and links.

**Rules:**
- content nonblank.
- difficulty uses existing `QuestionDifficulty`.
- links nonempty.
- each weight >0 and <=1.
- weights sum to 1.0 within 0.000001.
- exactly one `primary=true`.
- no duplicate knowledge id.
- every linked knowledge point belongs to the same exam.
- `AI_GENERATED` remains distinguishable from `REAL_EXAM`.

---

### Task 5: Answer record persistence and idempotent mastery application

**Files:**
- Test first: `learning/persistence/MasteryApplicationServiceIntegrationTest.java`
- Create:
  - `learning/model/AnswerSource.java`
  - `learning/persistence/AnswerRecordEntity.java`
  - `learning/persistence/UserKnowledgeMasteryEntity.java`
  - `learning/persistence/AnswerRecordMapper.java`
  - `learning/persistence/UserKnowledgeMasteryMapper.java`
  - `learning/persistence/MasteryDelta.java`
  - `learning/persistence/MasteryApplicationService.java`

**Interfaces:**
- persist answer records with UUID id.
- `boolean apply(UUID answerId, List<MasteryDelta> deltas)`

**Rules:**
- answer record is not deleted/overwritten by mastery application.
- application atomically claims `mastery_applied=false -> true`.
- only the transaction that successfully claims may mutate mastery.
- repeated call for same answer returns false and changes nothing.
- mastery rows are upserted and clamped 0..100.
- evidence_count increments exactly once per applied delta.
- last_effective_study_at updates on successful application.
- empty/duplicate deltas rejected.

---

### Task 6: Wrong-question ACTIVE / MASTERED lifecycle

**Files:**
- Test first: `question/WrongQuestionServiceIntegrationTest.java`
- Create:
  - `question/model/WrongQuestionStatus.java`
  - `question/persistence/WrongQuestionEntity.java`
  - `question/persistence/WrongQuestionMapper.java`
  - `question/WrongQuestionService.java`

**Interfaces:**
- `recordWrong(userId, questionId)`
- `recordCorrectReview(userId, questionId, effectiveMastery)`
- `find(userId, questionId)`

**Rules:**
- first wrong => ACTIVE, wrong_count=1, consecutive_correct=0.
- another wrong increments wrong_count, resets consecutive_correct, and reactivates.
- correct review increments consecutive_correct.
- transition to MASTERED only when consecutive_correct >=2 and effectiveMastery >=70.
- correct review below mastery threshold remains ACTIVE.
- history row remains after MASTERED.
- unique row per user/question.

---

### Task 7: Phase 2 quality gate

**Verification:**
- `mvn -B verify`
- all Phase 1 coverage gates still pass.
- PostgreSQL integration suite passes in CI.
- no H2 dependency.
- no REST controllers.
- no fake seed data.
- no Redis/AI/payment dependency.
- Flyway schema is the only schema initializer.

**Completion:** mark this plan completed and link it from `docs/implementation-plan.md`.
